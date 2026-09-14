package com.example.filmprop.service;

import com.example.filmprop.dto.request.CrewCreateRequest;
import com.example.filmprop.entity.Crew;
import com.example.filmprop.repository.CrewRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CrewService {
    private static final Logger log = LoggerFactory.getLogger(CrewService.class);
    
    private final CrewRepository crewRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public CrewService(CrewRepository crewRepository, RedisTemplate<String, Object> redisTemplate) {
        this.crewRepository = crewRepository;
        this.redisTemplate = redisTemplate;
    }
    
    private static final String CREW_HASH_KEY_PREFIX = "crew:info:";
    
    @Transactional
    public Crew createCrew(CrewCreateRequest request) {
        if (crewRepository.findByCrewName(request.getCrewName()).isPresent()) {
            throw new IllegalArgumentException("剧组名称已存在: " + request.getCrewName());
        }
        
        Crew crew = new Crew();
        crew.setCrewName(request.getCrewName());
        crew.setProjectName(request.getProjectName());
        crew.setDirector(request.getDirector());
        crew.setGenre(request.getGenre());
        crew.setStartDate(request.getStartDate());
        crew.setEndDate(request.getEndDate());
        crew.setStatus("active");
        
        Crew savedCrew = crewRepository.save(crew);
        cacheCrew(savedCrew);
        log.info("创建剧组: {}", savedCrew.getCrewName());
        return savedCrew;
    }
    
    public Crew getCrewById(Long id) {
        return crewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("剧组不存在: " + id));
    }
    
    public Crew getCrewByName(String crewName) {
        return crewRepository.findByCrewName(crewName)
                .orElseThrow(() -> new EntityNotFoundException("剧组不存在: " + crewName));
    }
    
    public List<Crew> getAllCrews() {
        return crewRepository.findAll();
    }
    
    public List<Crew> getCrewsByStatus(String status) {
        return crewRepository.findByStatus(status);
    }
    
    @Transactional
    public Crew updateCrew(Long id, CrewCreateRequest request) {
        Crew crew = getCrewById(id);
        
        if (!crew.getCrewName().equals(request.getCrewName()) &&
            crewRepository.findByCrewName(request.getCrewName()).isPresent()) {
            throw new IllegalArgumentException("剧组名称已存在: " + request.getCrewName());
        }
        
        crew.setCrewName(request.getCrewName());
        crew.setProjectName(request.getProjectName());
        crew.setDirector(request.getDirector());
        crew.setGenre(request.getGenre());
        crew.setStartDate(request.getStartDate());
        crew.setEndDate(request.getEndDate());
        
        Crew updatedCrew = crewRepository.save(crew);
        cacheCrew(updatedCrew);
        log.info("更新剧组: {}", updatedCrew.getCrewName());
        return updatedCrew;
    }
    
    @Transactional
    public void deleteCrew(Long id) {
        Crew crew = getCrewById(id);
        crewRepository.delete(crew);
        redisTemplate.delete(CREW_HASH_KEY_PREFIX + id);
        log.info("删除剧组: {}", crew.getCrewName());
    }
    
    private void cacheCrew(Crew crew) {
        String key = CREW_HASH_KEY_PREFIX + crew.getId();
        redisTemplate.opsForHash().put(key, "id", crew.getId());
        redisTemplate.opsForHash().put(key, "crewName", crew.getCrewName());
        redisTemplate.opsForHash().put(key, "projectName", crew.getProjectName());
        redisTemplate.opsForHash().put(key, "director", crew.getDirector());
        redisTemplate.opsForHash().put(key, "genre", crew.getGenre());
        redisTemplate.opsForHash().put(key, "startDate", crew.getStartDate());
        redisTemplate.opsForHash().put(key, "endDate", crew.getEndDate());
        redisTemplate.opsForHash().put(key, "status", crew.getStatus());
    }
    
    public Crew getCrewFromCache(Long id) {
        String key = CREW_HASH_KEY_PREFIX + id;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Crew crew = new Crew();
            crew.setId((Long) redisTemplate.opsForHash().get(key, "id"));
            crew.setCrewName((String) redisTemplate.opsForHash().get(key, "crewName"));
            crew.setProjectName((String) redisTemplate.opsForHash().get(key, "projectName"));
            crew.setDirector((String) redisTemplate.opsForHash().get(key, "director"));
            crew.setGenre((String) redisTemplate.opsForHash().get(key, "genre"));
            crew.setStartDate((java.time.LocalDate) redisTemplate.opsForHash().get(key, "startDate"));
            crew.setEndDate((java.time.LocalDate) redisTemplate.opsForHash().get(key, "endDate"));
            crew.setStatus((String) redisTemplate.opsForHash().get(key, "status"));
            return crew;
        }
        return null;
    }
}
