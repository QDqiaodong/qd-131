package com.example.filmprop.repository;

import com.example.filmprop.entity.OccupancyImportRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OccupancyImportRowRepository extends JpaRepository<OccupancyImportRow, Long> {
    List<OccupancyImportRow> findByBatchIdOrderByRowNo(Long batchId);

    List<OccupancyImportRow> findByBatchIdAndValidateStatusOrderByRowNo(Long batchId, String validateStatus);

    /**
     * 按批次查行，校验状态、写入状态均可选（传 null 不过滤）。
     * 例如 writeStatus = not_written 即筛出所有未写入的行。
     */
    @Query("SELECT r FROM OccupancyImportRow r WHERE r.batchId = :batchId " +
           "AND (:validateStatus IS NULL OR r.validateStatus = :validateStatus) " +
           "AND (:writeStatus IS NULL OR r.writeStatus = :writeStatus) " +
           "ORDER BY r.rowNo")
    List<OccupancyImportRow> findRows(@Param("batchId") Long batchId,
                                      @Param("validateStatus") String validateStatus,
                                      @Param("writeStatus") String writeStatus);
}
