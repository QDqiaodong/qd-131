package com.example.filmprop.service;

import com.example.filmprop.dto.request.BindingCreateRequest;
import com.example.filmprop.dto.request.BindingUpdateRequest;
import com.example.filmprop.dto.response.BindingDetailResponse;
import com.example.filmprop.dto.response.ConflictCheckResponse;
import com.example.filmprop.entity.Crew;
import com.example.filmprop.entity.Prop;
import com.example.filmprop.entity.PropScheduleBinding;
import com.example.filmprop.entity.ScheduleChangeLog;
import com.example.filmprop.repository.PropScheduleBindingRepository;
import com.example.filmprop.repository.ScheduleChangeLogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleBindingService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleBindingService.class);
    
    private final PropScheduleBindingRepository bindingRepository;
    private final ScheduleChangeLogRepository changeLogRepository;
    private final PropService propService;
    private final CrewService crewService;
    
    public ScheduleBindingService(PropScheduleBindingRepository bindingRepository, 
                                  ScheduleChangeLogRepository changeLogRepository,
                                  PropService propService, CrewService crewService) {
        this.bindingRepository = bindingRepository;
        this.changeLogRepository = changeLogRepository;
        this.propService = propService;
        this.crewService = crewService;
    }
    
    @Transactional
    public BindingDetailResponse createBinding(BindingCreateRequest request) {
        Prop prop = propService.getPropById(request.getPropId());
        Crew crew = crewService.getCrewById(request.getCrewId());

        // 片种核对：剧组片种与道具场景类型不一致时拦截，绑定不落库
        checkGenreMatch(prop, crew);

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        
        ConflictCheckResponse conflict = checkConflict(request.getPropId(), request.getStartDate(), request.getEndDate(), -1L);
        if (conflict.isHasConflict()) {
            throw new IllegalArgumentException("档期时间冲突: " + conflict.getConflictMessage());
        }
        
        PropScheduleBinding binding = new PropScheduleBinding();
        binding.setPropId(request.getPropId());
        binding.setCrewId(request.getCrewId());
        binding.setStartDate(request.getStartDate());
        binding.setEndDate(request.getEndDate());
        binding.setBindingType(request.getBindingType());
        binding.setRemark(request.getRemark());
        binding.setStatus("active");
        
        PropScheduleBinding savedBinding = bindingRepository.save(binding);
        log.info("创建道具绑定: propId={}, crewId={}", request.getPropId(), request.getCrewId());
        
        saveChangeLog(savedBinding.getId(), request.getPropId(), request.getCrewId(),
                null, null, request.getStartDate(), request.getEndDate(),
                "create", request.getRemark(), request.getOperator(), false, null);
        
        prop.setStatus("in_use");
        propService.updateProp(prop.getId(), null);
        
        return buildBindingDetail(savedBinding, prop, crew);
    }
    
    /**
     * 片种核对：剧组片种必须与道具场景类型一致，跨片种绑定一律拦截。
     * 剧组未登记片种时无法完成核对，按核对不通过处理，同样拦截。
     */
    private void checkGenreMatch(Prop prop, Crew crew) {
        String crewGenre = crew.getGenre();
        if (crewGenre == null || crewGenre.isBlank()) {
            log.warn("绑定被拦截，剧组未登记片种: propId={}, crewId={}", prop.getId(), crew.getId());
            throw new IllegalArgumentException(String.format(
                    "片种核对失败：剧组【%s】未登记片种，无法与道具【%s】（场景类型：%s）核对，请先完善剧组片种信息",
                    crew.getCrewName(), prop.getPropName(), prop.getSceneType()));
        }
        if (!crewGenre.equals(prop.getSceneType())) {
            log.warn("绑定被拦截，片种不符: propId={}, sceneType={}, crewId={}, genre={}",
                    prop.getId(), prop.getSceneType(), crew.getId(), crewGenre);
            throw new IllegalArgumentException(String.format(
                    "片种不符：剧组【%s】片种为【%s】，道具【%s】适用场景为【%s】，跨片种绑定已拦截",
                    crew.getCrewName(), crewGenre, prop.getPropName(), prop.getSceneType()));
        }
    }

    public ConflictCheckResponse checkConflict(Long propId, LocalDate startDate, LocalDate endDate, Long excludeBindingId) {
        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setHasConflict(false);
        
        List<PropScheduleBinding> conflictingBindings = bindingRepository.findConflictingBindings(
                propId, startDate, endDate, excludeBindingId);
        
        if (!conflictingBindings.isEmpty()) {
            PropScheduleBinding conflict = conflictingBindings.get(0);
            Crew crew = crewService.getCrewById(conflict.getCrewId());
            
            response.setHasConflict(true);
            response.setConflictingBindingId(conflict.getId());
            response.setConflictingCrewName(crew.getCrewName());
            response.setConflictingStartDate(conflict.getStartDate());
            response.setConflictingEndDate(conflict.getEndDate());
            response.setConflictMessage(String.format(
                    "道具已被【%s】占用，占用时间：%s 至 %s",
                    crew.getCrewName(), conflict.getStartDate(), conflict.getEndDate()));
        }
        
        return response;
    }
    
    @Transactional
    public BindingDetailResponse updateBinding(BindingUpdateRequest request) {
        PropScheduleBinding binding = bindingRepository.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("绑定记录不存在: " + request.getId()));
        
        Prop prop = propService.getPropById(binding.getPropId());
        Crew crew = crewService.getCrewById(binding.getCrewId());
        
        LocalDate originalStartDate = binding.getStartDate();
        LocalDate originalEndDate = binding.getEndDate();
        
        boolean dateChanged = request.getStartDate() != null || request.getEndDate() != null;
        LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : originalStartDate;
        LocalDate newEndDate = request.getEndDate() != null ? request.getEndDate() : originalEndDate;
        
        if (dateChanged) {
            if (newStartDate.isAfter(newEndDate)) {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
            
            ConflictCheckResponse conflict = checkConflict(binding.getPropId(), newStartDate, newEndDate, request.getId());
            if (conflict.isHasConflict()) {
                saveChangeLog(request.getId(), binding.getPropId(), binding.getCrewId(),
                        originalStartDate, originalEndDate, newStartDate, newEndDate,
                        "update", request.getChangeReason(), request.getOperator(), true, conflict.getConflictMessage());
                throw new IllegalArgumentException("档期时间冲突: " + conflict.getConflictMessage());
            }
            
            binding.setStartDate(newStartDate);
            binding.setEndDate(newEndDate);
        }
        
        if (request.getBindingType() != null) {
            binding.setBindingType(request.getBindingType());
        }
        if (request.getRemark() != null) {
            binding.setRemark(request.getRemark());
        }
        
        PropScheduleBinding updatedBinding = bindingRepository.save(binding);
        log.info("更新道具绑定: bindingId={}", request.getId());
        
        saveChangeLog(request.getId(), binding.getPropId(), binding.getCrewId(),
                originalStartDate, originalEndDate, updatedBinding.getStartDate(), updatedBinding.getEndDate(),
                "update", request.getChangeReason(), request.getOperator(), false, null);
        
        return buildBindingDetail(updatedBinding, prop, crew);
    }
    
    @Transactional
    public void cancelBinding(Long id, String reason, String operator) {
        PropScheduleBinding binding = bindingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("绑定记录不存在: " + id));
        
        binding.setStatus("cancelled");
        bindingRepository.save(binding);
        
        saveChangeLog(id, binding.getPropId(), binding.getCrewId(),
                binding.getStartDate(), binding.getEndDate(), binding.getStartDate(), binding.getEndDate(),
                "cancel", reason, operator, false, null);
        
        updatePropStatusAfterCancel(binding.getPropId());
        
        log.info("取消道具绑定: bindingId={}", id);
    }
    
    private void updatePropStatusAfterCancel(Long propId) {
        List<PropScheduleBinding> activeBindings = bindingRepository.findByPropId(propId).stream()
                .filter(b -> "active".equals(b.getStatus()))
                .collect(Collectors.toList());
        
        Prop prop = propService.getPropById(propId);
        if (activeBindings.isEmpty()) {
            prop.setStatus("available");
        }
        propService.updateProp(propId, null);
    }
    
    private void saveChangeLog(Long bindingId, Long propId, Long crewId,
                               LocalDate originalStartDate, LocalDate originalEndDate,
                               LocalDate newStartDate, LocalDate newEndDate,
                               String changeType, String changeReason, String operator,
                               Boolean conflictDetected, String conflictDescription) {
        ScheduleChangeLog logEntry = new ScheduleChangeLog();
        logEntry.setBindingId(bindingId);
        logEntry.setPropId(propId);
        logEntry.setCrewId(crewId);
        logEntry.setOriginalStartDate(originalStartDate);
        logEntry.setOriginalEndDate(originalEndDate);
        logEntry.setNewStartDate(newStartDate);
        logEntry.setNewEndDate(newEndDate);
        logEntry.setChangeType(changeType);
        logEntry.setChangeReason(changeReason);
        logEntry.setOperator(operator);
        logEntry.setConflictDetected(conflictDetected);
        logEntry.setConflictDescription(conflictDescription);
        
        changeLogRepository.save(logEntry);
    }
    
    public List<BindingDetailResponse> getAllBindings() {
        return bindingRepository.findAll().stream()
                .map(this::buildBindingDetail)
                .collect(Collectors.toList());
    }
    
    public List<BindingDetailResponse> getBindingsByPropId(Long propId) {
        return bindingRepository.findByPropId(propId).stream()
                .map(this::buildBindingDetail)
                .collect(Collectors.toList());
    }
    
    public List<BindingDetailResponse> getBindingsByCrewId(Long crewId) {
        return bindingRepository.findByCrewId(crewId).stream()
                .map(this::buildBindingDetail)
                .collect(Collectors.toList());
    }
    
    public List<BindingDetailResponse> getBindingsByCrewName(String crewName) {
        Crew crew = crewService.getCrewByName(crewName);
        return getBindingsByCrewId(crew.getId());
    }
    
    public List<BindingDetailResponse> getBindingsInDateRange(LocalDate startDate, LocalDate endDate) {
        return bindingRepository.findBindingsInDateRange(startDate, endDate).stream()
                .map(this::buildBindingDetail)
                .collect(Collectors.toList());
    }
    
    public BindingDetailResponse getBindingById(Long id) {
        PropScheduleBinding binding = bindingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("绑定记录不存在: " + id));
        return buildBindingDetail(binding);
    }
    
    private BindingDetailResponse buildBindingDetail(PropScheduleBinding binding) {
        Prop prop = propService.getPropById(binding.getPropId());
        Crew crew = crewService.getCrewById(binding.getCrewId());
        return buildBindingDetail(binding, prop, crew);
    }
    
    private BindingDetailResponse buildBindingDetail(PropScheduleBinding binding, Prop prop, Crew crew) {
        BindingDetailResponse response = new BindingDetailResponse();
        response.setId(binding.getId());
        response.setPropId(prop.getId());
        response.setPropCode(prop.getPropCode());
        response.setPropName(prop.getPropName());
        response.setSceneType(prop.getSceneType());
        response.setCrewId(crew.getId());
        response.setCrewName(crew.getCrewName());
        response.setCrewGenre(crew.getGenre());
        response.setProjectName(crew.getProjectName());
        response.setStartDate(binding.getStartDate());
        response.setEndDate(binding.getEndDate());
        response.setStatus(binding.getStatus());
        response.setBindingType(binding.getBindingType());
        response.setRemark(binding.getRemark());
        response.setCreatedAt(binding.getCreatedAt());
        response.setUpdatedAt(binding.getUpdatedAt());
        return response;
    }
    
    public List<ScheduleChangeLog> getAllChangeLogs() {
        return changeLogRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<ScheduleChangeLog> getChangeLogsByBindingId(Long bindingId) {
        return changeLogRepository.findByBindingId(bindingId);
    }
    
    public List<ScheduleChangeLog> getChangeLogsWithConflicts() {
        return changeLogRepository.findByConflictDetected(true);
    }
}
