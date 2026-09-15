package com.example.filmprop.controller;

import com.example.filmprop.dto.response.ApiResponse;
import com.example.filmprop.dto.response.ImportBatchResponse;
import com.example.filmprop.dto.response.ImportBatchResultResponse;
import com.example.filmprop.dto.response.ImportRowResponse;
import com.example.filmprop.service.OccupancyImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/occupancy-import")
public class OccupancyImportController {
    private final OccupancyImportService importService;

    public OccupancyImportController(OccupancyImportService importService) {
        this.importService = importService;
    }

    /**
     * 第一步（核）：上传 CSV 逐行校验，结果落库但不写绑定。
     */
    @PostMapping("/validate")
    public ApiResponse<ImportBatchResultResponse> validate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String operator) {
        return ApiResponse.success("校验完成", importService.validate(file, operator));
    }

    /**
     * 第二步（写）：仅校验通过的行写入绑定表，未通过的行落为未写入。
     */
    @PostMapping("/{batchId}/commit")
    public ApiResponse<ImportBatchResultResponse> commit(
            @PathVariable Long batchId,
            @RequestParam(required = false) String operator) {
        return ApiResponse.success("写入完成", importService.commit(batchId, operator));
    }

    @GetMapping("/batches")
    public ApiResponse<List<ImportBatchResponse>> getBatches() {
        return ApiResponse.success(importService.getBatches());
    }

    /**
     * 批次行查询：validateStatus / writeStatus 均可选，
     * writeStatus=not_written 即筛出全部未写入行。
     */
    @GetMapping("/batches/{batchId}/rows")
    public ApiResponse<List<ImportRowResponse>> getRows(
            @PathVariable Long batchId,
            @RequestParam(required = false) String validateStatus,
            @RequestParam(required = false) String writeStatus) {
        return ApiResponse.success(importService.getRows(batchId, validateStatus, writeStatus));
    }
}
