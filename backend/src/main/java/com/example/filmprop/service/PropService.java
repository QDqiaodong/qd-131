package com.example.filmprop.service;

import com.example.filmprop.dto.request.PropCreateRequest;
import com.example.filmprop.entity.Prop;
import com.example.filmprop.repository.PropRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PropService {
    private static final Logger log = LoggerFactory.getLogger(PropService.class);
    
    private final PropRepository propRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public PropService(PropRepository propRepository, RedisTemplate<String, Object> redisTemplate) {
        this.propRepository = propRepository;
        this.redisTemplate = redisTemplate;
    }
    
    private static final String PROP_HASH_KEY_PREFIX = "prop:spec:";
    
    @Transactional
    public Prop createProp(PropCreateRequest request) {
        if (propRepository.findByPropCode(request.getPropCode()).isPresent()) {
            throw new IllegalArgumentException("道具编号已存在: " + request.getPropCode());
        }
        
        Prop prop = new Prop();
        prop.setPropCode(request.getPropCode());
        prop.setPropName(request.getPropName());
        prop.setSceneType(request.getSceneType());
        prop.setMaterial(request.getMaterial());
        prop.setSpecification(request.getSpecification());
        prop.setQuantity(request.getQuantity());
        prop.setStatus("available");
        
        Prop savedProp = propRepository.save(prop);
        cacheProp(savedProp);
        log.info("创建道具: {}", savedProp.getPropCode());
        return savedProp;
    }
    
    public Prop getPropById(Long id) {
        return propRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("道具不存在: " + id));
    }
    
    public Prop getPropByCode(String propCode) {
        return propRepository.findByPropCode(propCode)
                .orElseThrow(() -> new EntityNotFoundException("道具不存在: " + propCode));
    }
    
    public List<Prop> getAllProps() {
        return propRepository.findAll();
    }
    
    public List<Prop> getPropsBySceneType(String sceneType) {
        return propRepository.findBySceneType(sceneType);
    }
    
    public List<Prop> getPropsByStatus(String status) {
        return propRepository.findByStatus(status);
    }
    
    @Transactional
    public Prop updateProp(Long id, PropCreateRequest request) {
        Prop prop = getPropById(id);
        
        if (request != null) {
            if (!prop.getPropCode().equals(request.getPropCode()) &&
                propRepository.findByPropCode(request.getPropCode()).isPresent()) {
                throw new IllegalArgumentException("道具编号已存在: " + request.getPropCode());
            }
            
            prop.setPropCode(request.getPropCode());
            prop.setPropName(request.getPropName());
            prop.setSceneType(request.getSceneType());
            prop.setMaterial(request.getMaterial());
            prop.setSpecification(request.getSpecification());
            prop.setQuantity(request.getQuantity());
        }
        
        Prop updatedProp = propRepository.save(prop);
        cacheProp(updatedProp);
        log.info("更新道具: {}", updatedProp.getPropCode());
        return updatedProp;
    }
    
    @Transactional
    public void deleteProp(Long id) {
        Prop prop = getPropById(id);
        propRepository.delete(prop);
        redisTemplate.delete(PROP_HASH_KEY_PREFIX + id);
        log.info("删除道具: {}", prop.getPropCode());
    }
    
    private void cacheProp(Prop prop) {
        String key = PROP_HASH_KEY_PREFIX + prop.getId();
        redisTemplate.opsForHash().put(key, "id", prop.getId());
        redisTemplate.opsForHash().put(key, "propCode", prop.getPropCode());
        redisTemplate.opsForHash().put(key, "propName", prop.getPropName());
        redisTemplate.opsForHash().put(key, "sceneType", prop.getSceneType());
        redisTemplate.opsForHash().put(key, "material", prop.getMaterial());
        redisTemplate.opsForHash().put(key, "specification", prop.getSpecification());
        redisTemplate.opsForHash().put(key, "quantity", prop.getQuantity());
        redisTemplate.opsForHash().put(key, "status", prop.getStatus());
    }
    
    public Prop getPropFromCache(Long id) {
        String key = PROP_HASH_KEY_PREFIX + id;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Prop prop = new Prop();
            prop.setId((Long) redisTemplate.opsForHash().get(key, "id"));
            prop.setPropCode((String) redisTemplate.opsForHash().get(key, "propCode"));
            prop.setPropName((String) redisTemplate.opsForHash().get(key, "propName"));
            prop.setSceneType((String) redisTemplate.opsForHash().get(key, "sceneType"));
            prop.setMaterial((String) redisTemplate.opsForHash().get(key, "material"));
            prop.setSpecification((String) redisTemplate.opsForHash().get(key, "specification"));
            prop.setQuantity((Integer) redisTemplate.opsForHash().get(key, "quantity"));
            prop.setStatus((String) redisTemplate.opsForHash().get(key, "status"));
            return prop;
        }
        return null;
    }
}
