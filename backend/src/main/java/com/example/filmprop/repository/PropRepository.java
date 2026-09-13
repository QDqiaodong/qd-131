package com.example.filmprop.repository;

import com.example.filmprop.entity.Prop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropRepository extends JpaRepository<Prop, Long> {
    Optional<Prop> findByPropCode(String propCode);
    List<Prop> findBySceneType(String sceneType);
    List<Prop> findByStatus(String status);
    List<Prop> findByPropNameContaining(String propName);
}
