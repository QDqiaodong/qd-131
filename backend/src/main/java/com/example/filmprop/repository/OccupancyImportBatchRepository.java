package com.example.filmprop.repository;

import com.example.filmprop.entity.OccupancyImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OccupancyImportBatchRepository extends JpaRepository<OccupancyImportBatch, Long> {
    List<OccupancyImportBatch> findAllByOrderByCreatedAtDesc();

    /**
     * 条件更新：仅当批次仍处于「已校验待写入」时置为「已写入」。
     * 返回 0 表示批次不存在或已被提交过，用于拦截重复写入。
     */
    @Modifying
    @Query("UPDATE OccupancyImportBatch b SET b.status = 'committed', b.committedAt = :committedAt " +
           "WHERE b.id = :id AND b.status = 'validated'")
    int markCommitted(@Param("id") Long id, @Param("committedAt") LocalDateTime committedAt);
}
