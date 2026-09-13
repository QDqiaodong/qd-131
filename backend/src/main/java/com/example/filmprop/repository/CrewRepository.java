package com.example.filmprop.repository;

import com.example.filmprop.entity.Crew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrewRepository extends JpaRepository<Crew, Long> {
    Optional<Crew> findByCrewName(String crewName);
    List<Crew> findByStatus(String status);
    List<Crew> findByCrewNameContaining(String crewName);
}
