package com.example.filmprop.service;

import com.example.filmprop.dto.request.BindingCreateRequest;
import com.example.filmprop.dto.response.ImportBatchResponse;
import com.example.filmprop.dto.response.ImportBatchResultResponse;
import com.example.filmprop.dto.response.ImportRowResponse;
import com.example.filmprop.entity.Crew;
import com.example.filmprop.entity.OccupancyImportBatch;
import com.example.filmprop.entity.OccupancyImportRow;
import com.example.filmprop.entity.Prop;
import com.example.filmprop.entity.PropScheduleBinding;
import com.example.filmprop.repository.CrewRepository;
import com.example.filmprop.repository.OccupancyImportBatchRepository;
import com.example.filmprop.repository.OccupancyImportRowRepository;
import com.example.filmprop.repository.PropRepository;
import com.example.filmprop.repository.PropScheduleBindingRepository;
import com.example.filmprop.util.CsvParser;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 道具占用批量导入。
 * 流程固定为先核后写：validate 只校验不落绑定，commit 只放行校验通过的行写入绑定表，
 * 校验未通过的行一律标记 not_written，不进入写入。
 */
@Service
public class OccupancyImportService {
    private static final Logger log = LoggerFactory.getLogger(OccupancyImportService.class);

    private static final Set<String> VALIDATE_STATUSES = Set.of("passed", "failed");
    private static final Set<String> WRITE_STATUSES = Set.of("pending", "written", "not_written");

    private final OccupancyImportBatchRepository batchRepository;
    private final OccupancyImportRowRepository rowRepository;
    private final PropRepository propRepository;
    private final CrewRepository crewRepository;
    private final PropScheduleBindingRepository bindingRepository;
    private final ScheduleBindingService bindingService;
    private final TransactionTemplate transactionTemplate;

    public OccupancyImportService(OccupancyImportBatchRepository batchRepository,
                                  OccupancyImportRowRepository rowRepository,
                                  PropRepository propRepository,
                                  CrewRepository crewRepository,
                                  PropScheduleBindingRepository bindingRepository,
                                  ScheduleBindingService bindingService,
                                  PlatformTransactionManager transactionManager) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.propRepository = propRepository;
        this.crewRepository = crewRepository;
        this.bindingRepository = bindingRepository;
        this.bindingService = bindingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 第一步（核）：解析 CSV 并逐行校验，批次与行结果落库，不写任何绑定。
     */
    @Transactional
    public ImportBatchResultResponse validate(MultipartFile file, String operator) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空，请选择 CSV 文件");
        }

        List<List<String>> csvRows;
        try {
            csvRows = CsvParser.parse(file.getInputStream());
        } catch (IOException e) {
            log.warn("CSV 读取失败: {}", e.getMessage());
            throw new IllegalArgumentException("CSV 文件读取失败，请确认文件未损坏后重试");
        }

        List<OccupancyImportRow> rows = buildRows(csvRows);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV 中没有可导入的数据行，请核对文件内容");
        }

        // 单行校验：必填、日期、起止顺序、道具/剧组存在性、片种核对、库内档期冲突
        for (OccupancyImportRow row : rows) {
            validateRow(row);
        }
        // 文件内部核对：同一道具多行日期重叠的，相关行一并拦截
        checkInternalConflicts(rows);

        int passed = (int) rows.stream().filter(r -> "passed".equals(r.getValidateStatus())).count();

        OccupancyImportBatch batch = new OccupancyImportBatch();
        batch.setFileName(file.getOriginalFilename() == null ? "occupancy.csv" : file.getOriginalFilename());
        batch.setOperator(operator == null || operator.isBlank() ? "system" : operator.trim());
        batch.setTotalRows(rows.size());
        batch.setPassedRows(passed);
        batch.setFailedRows(rows.size() - passed);
        batch.setStatus("validated");

        OccupancyImportBatch savedBatch = batchRepository.save(batch);
        for (OccupancyImportRow row : rows) {
            row.setBatchId(savedBatch.getId());
        }
        rowRepository.saveAll(rows);
        log.info("占用导入校验完成: batchId={}, 总行数={}, 通过={}, 未通过={}",
                savedBatch.getId(), rows.size(), passed, rows.size() - passed);

        return buildResult(savedBatch, rows);
    }

    /**
     * 第二步（写）：仅放行校验通过的行逐行写入绑定表。
     * 校验未通过的行直接标记 not_written，不进入写入；批次只允许提交一次。
     */
    public ImportBatchResultResponse commit(Long batchId, String operator) {
        // 条件更新占位：批次不存在或已提交过时返回 0，拦截重复写入
        Boolean marked = transactionTemplate.execute(status ->
                batchRepository.markCommitted(batchId, LocalDateTime.now()) > 0);
        if (!Boolean.TRUE.equals(marked)) {
            throw new IllegalArgumentException("批次不存在或已完成写入，不能重复提交: " + batchId);
        }

        List<OccupancyImportRow> rows = rowRepository.findByBatchIdOrderByRowNo(batchId);
        String writeOperator = operator == null || operator.isBlank() ? "system" : operator.trim();

        for (OccupancyImportRow row : rows) {
            if (!"passed".equals(row.getValidateStatus())) {
                // 上一步（校验）未过的行不能往后走：直接落未写入，原因沿用校验结论
                row.setWriteStatus("not_written");
                row.setWriteMessage("校验未通过，未进入写入：" + row.getValidateMessage());
                continue;
            }
            writeRow(row, writeOperator);
        }
        rowRepository.saveAll(rows);

        int written = (int) rows.stream().filter(r -> "written".equals(r.getWriteStatus())).count();
        transactionTemplate.execute(status -> {
            OccupancyImportBatch batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new EntityNotFoundException("导入批次不存在: " + batchId));
            batch.setWrittenRows(written);
            batch.setNotWrittenRows(rows.size() - written);
            batchRepository.save(batch);
            return null;
        });
        log.info("占用导入写入完成: batchId={}, 已写入={}, 未写入={}", batchId, written, rows.size() - written);

        OccupancyImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("导入批次不存在: " + batchId));
        return buildResult(batch, rows);
    }

    /**
     * 单行写入：复用单条绑定的完整规则（片种核对、档期冲突兜底、变更日志、道具状态联动），
     * 每行独立事务，单行失败不影响其他行。
     */
    private void writeRow(OccupancyImportRow row, String operator) {
        try {
            BindingCreateRequest request = new BindingCreateRequest();
            request.setPropId(row.getPropId());
            request.setCrewId(row.getCrewId());
            request.setStartDate(row.getStartDate());
            request.setEndDate(row.getEndDate());
            request.setBindingType("formal");
            request.setRemark(row.getRemark());
            request.setOperator(operator);

            Long bindingId = bindingService.createBinding(request).getId();
            row.setWriteStatus("written");
            row.setWriteMessage("写入成功");
            row.setBindingId(bindingId);
        } catch (Exception e) {
            // 校验通过到写入之间库内数据可能已变化，写入阶段的兜底拦截同样落为未写入
            log.warn("占用导入行写入失败: rowId={}, 原因={}", row.getId(), e.getMessage());
            row.setWriteStatus("not_written");
            row.setWriteMessage("写入被拦截：" + e.getMessage());
        }
    }

    public List<ImportBatchResponse> getBatches() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::buildBatchResponse)
                .collect(Collectors.toList());
    }

    /**
     * 按批次查行。validateStatus、writeStatus 传 null 表示不过滤，
     * 例如 writeStatus = not_written 即筛出全部未写入行。
     */
    public List<ImportRowResponse> getRows(Long batchId, String validateStatus, String writeStatus) {
        if (validateStatus != null && !VALIDATE_STATUSES.contains(validateStatus)) {
            throw new IllegalArgumentException("不支持的校验状态筛选值: " + validateStatus);
        }
        if (writeStatus != null && !WRITE_STATUSES.contains(writeStatus)) {
            throw new IllegalArgumentException("不支持的写入状态筛选值: " + writeStatus);
        }
        if (!batchRepository.existsById(batchId)) {
            throw new EntityNotFoundException("导入批次不存在: " + batchId);
        }
        return rowRepository.findRows(batchId, validateStatus, writeStatus).stream()
                .map(this::buildRowResponse)
                .collect(Collectors.toList());
    }

    /**
     * 把 CSV 行解析为待校验行：识别表头、跳过空行、按列取值。
     */
    private List<OccupancyImportRow> buildRows(List<List<String>> csvRows) {
        List<OccupancyImportRow> rows = new ArrayList<>();
        boolean headerChecked = false;
        int rowNo = 0;

        for (List<String> csvRow : csvRows) {
            if (!headerChecked) {
                headerChecked = true;
                String firstCell = column(csvRow, 0);
                if (firstCell.contains("道具编号") || firstCell.equalsIgnoreCase("prop_code")) {
                    continue;
                }
            }
            boolean blank = csvRow.stream().allMatch(c -> c == null || c.isBlank());
            if (blank) {
                continue;
            }

            rowNo++;
            OccupancyImportRow row = new OccupancyImportRow();
            row.setRowNo(rowNo);
            row.setPropCode(column(csvRow, 0));
            row.setCrewName(column(csvRow, 1));
            row.setStartDateRaw(column(csvRow, 2));
            row.setEndDateRaw(column(csvRow, 3));
            row.setRemark(column(csvRow, 4));
            rows.add(row);
        }
        return rows;
    }

    private String column(List<String> csvRow, int index) {
        if (index >= csvRow.size()) {
            return "";
        }
        String value = csvRow.get(index);
        return value == null ? "" : value.trim();
    }

    /**
     * 单行校验，按顺序执行，首个不通过即落结论并停止该行后续检查。
     */
    private void validateRow(OccupancyImportRow row) {
        if (row.getPropCode().isEmpty()) {
            fail(row, "道具编号不能为空");
            return;
        }
        if (row.getCrewName().isEmpty()) {
            fail(row, "剧组名不能为空");
            return;
        }
        if (row.getStartDateRaw().isEmpty() || row.getEndDateRaw().isEmpty()) {
            fail(row, "开始日期、结束日期不能为空");
            return;
        }

        LocalDate startDate = parseDate(row.getStartDateRaw());
        if (startDate == null) {
            fail(row, "开始日期格式不正确: " + row.getStartDateRaw() + "，应为 yyyy-MM-dd");
            return;
        }
        LocalDate endDate = parseDate(row.getEndDateRaw());
        if (endDate == null) {
            fail(row, "结束日期格式不正确: " + row.getEndDateRaw() + "，应为 yyyy-MM-dd");
            return;
        }
        row.setStartDate(startDate);
        row.setEndDate(endDate);

        if (startDate.isAfter(endDate)) {
            fail(row, "开始日期不能晚于结束日期");
            return;
        }

        Optional<Prop> propOpt = propRepository.findByPropCode(row.getPropCode());
        if (propOpt.isEmpty()) {
            fail(row, "道具编号不存在: " + row.getPropCode());
            return;
        }
        Prop prop = propOpt.get();
        row.setPropId(prop.getId());

        Optional<Crew> crewOpt = crewRepository.findByCrewName(row.getCrewName());
        if (crewOpt.isEmpty()) {
            fail(row, "剧组不存在: " + row.getCrewName());
            return;
        }
        Crew crew = crewOpt.get();
        row.setCrewId(crew.getId());

        // 片种核对：与单条绑定同一规则，剧组片种必须与道具场景类型一致
        if (crew.getGenre() == null || crew.getGenre().isBlank()) {
            fail(row, String.format("片种核对失败：剧组【%s】未登记片种，请先完善剧组片种信息", crew.getCrewName()));
            return;
        }
        if (!crew.getGenre().equals(prop.getSceneType())) {
            fail(row, String.format("片种不符：剧组【%s】片种为【%s】，道具【%s】适用场景为【%s】",
                    crew.getCrewName(), crew.getGenre(), prop.getPropName(), prop.getSceneType()));
            return;
        }

        // 库内档期冲突：与现有生效中的绑定日期重叠即拦截
        List<PropScheduleBinding> conflicts = bindingRepository.findConflictingBindings(
                prop.getId(), startDate, endDate, -1L);
        if (!conflicts.isEmpty()) {
            PropScheduleBinding conflict = conflicts.get(0);
            String crewName = crewRepository.findById(conflict.getCrewId())
                    .map(Crew::getCrewName).orElse("未知剧组");
            fail(row, String.format("档期冲突：道具已被【%s】占用（%s 至 %s）",
                    crewName, conflict.getStartDate(), conflict.getEndDate()));
            return;
        }

        row.setValidateStatus("passed");
        row.setValidateMessage("校验通过");
    }

    /**
     * 文件内部核对：同一道具的多行日期两两重叠时，相关行全部拦截，
     * 避免同一份 CSV 自相冲突的行先后写入。
     */
    private void checkInternalConflicts(List<OccupancyImportRow> rows) {
        Map<Long, List<OccupancyImportRow>> rowsByProp = rows.stream()
                .filter(r -> "passed".equals(r.getValidateStatus()))
                .collect(Collectors.groupingBy(OccupancyImportRow::getPropId, LinkedHashMap::new, Collectors.toList()));

        for (List<OccupancyImportRow> group : rowsByProp.values()) {
            Map<Integer, Set<Integer>> conflictPairs = new LinkedHashMap<>();
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    OccupancyImportRow a = group.get(i);
                    OccupancyImportRow b = group.get(j);
                    if (!a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate())) {
                        conflictPairs.computeIfAbsent(a.getRowNo(), k -> new TreeSet<>()).add(b.getRowNo());
                        conflictPairs.computeIfAbsent(b.getRowNo(), k -> new TreeSet<>()).add(a.getRowNo());
                    }
                }
            }
            for (OccupancyImportRow row : group) {
                Set<Integer> others = conflictPairs.get(row.getRowNo());
                if (others != null) {
                    fail(row, "文件内档期重叠：与第 " + others.stream()
                            .map(String::valueOf).collect(Collectors.joining("、")) + " 行占用同一道具且日期重叠");
                }
            }
        }
    }

    private void fail(OccupancyImportRow row, String message) {
        row.setValidateStatus("failed");
        row.setValidateMessage(message);
    }

    /**
     * 解析日期，支持 yyyy-MM-dd 与 yyyy/M/d，非法日期返回 null。
     */
    private LocalDate parseDate(String raw) {
        String[] parts = raw.trim().replace('/', '-').split("-");
        if (parts.length != 3) {
            return null;
        }
        try {
            return LocalDate.of(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private ImportBatchResultResponse buildResult(OccupancyImportBatch batch, List<OccupancyImportRow> rows) {
        return new ImportBatchResultResponse(
                buildBatchResponse(batch),
                rows.stream().map(this::buildRowResponse).collect(Collectors.toList()));
    }

    private ImportBatchResponse buildBatchResponse(OccupancyImportBatch batch) {
        ImportBatchResponse response = new ImportBatchResponse();
        response.setId(batch.getId());
        response.setFileName(batch.getFileName());
        response.setOperator(batch.getOperator());
        response.setTotalRows(batch.getTotalRows());
        response.setPassedRows(batch.getPassedRows());
        response.setFailedRows(batch.getFailedRows());
        response.setWrittenRows(batch.getWrittenRows());
        response.setNotWrittenRows(batch.getNotWrittenRows());
        response.setStatus(batch.getStatus());
        response.setCreatedAt(batch.getCreatedAt());
        response.setCommittedAt(batch.getCommittedAt());
        return response;
    }

    private ImportRowResponse buildRowResponse(OccupancyImportRow row) {
        ImportRowResponse response = new ImportRowResponse();
        response.setId(row.getId());
        response.setBatchId(row.getBatchId());
        response.setRowNo(row.getRowNo());
        response.setPropCode(row.getPropCode());
        response.setCrewName(row.getCrewName());
        response.setStartDateRaw(row.getStartDateRaw());
        response.setEndDateRaw(row.getEndDateRaw());
        response.setRemark(row.getRemark());
        response.setValidateStatus(row.getValidateStatus());
        response.setValidateMessage(row.getValidateMessage());
        response.setWriteStatus(row.getWriteStatus());
        response.setWriteMessage(row.getWriteMessage());
        response.setBindingId(row.getBindingId());
        return response;
    }
}
