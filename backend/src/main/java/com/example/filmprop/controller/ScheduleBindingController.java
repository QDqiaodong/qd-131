package com.example.filmprop.controller;

import com.example.filmprop.dto.request.BindingCreateRequest;
import com.example.filmprop.dto.request.BindingUpdateRequest;
import com.example.filmprop.dto.response.ApiResponse;
import com.example.filmprop.dto.response.BindingDetailResponse;
import com.example.filmprop.dto.response.ConflictCheckResponse;
import com.example.filmprop.entity.ScheduleChangeLog;
import com.example.filmprop.service.ScheduleBindingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bindings")
public class ScheduleBindingController {
    private final ScheduleBindingService bindingService;
    
    public ScheduleBindingController(ScheduleBindingService bindingService) {
        this.bindingService = bindingService;
    }
    
    @PostMapping
    public ApiResponse<BindingDetailResponse> createBinding(@Valid @RequestBody BindingCreateRequest request) {
        return ApiResponse.success(bindingService.createBinding(request));
    }
    
    @GetMapping
    public ApiResponse<List<BindingDetailResponse>> getAllBindings() {
        return ApiResponse.success(bindingService.getAllBindings());
    }
    
    @GetMapping("/{id}")
    public ApiResponse<BindingDetailResponse> getBindingById(@PathVariable Long id) {
        return ApiResponse.success(bindingService.getBindingById(id));
    }
    
    @GetMapping("/prop/{propId}")
    public ApiResponse<List<BindingDetailResponse>> getBindingsByPropId(@PathVariable Long propId) {
        return ApiResponse.success(bindingService.getBindingsByPropId(propId));
    }
    
    @GetMapping("/crew/{crewId}")
    public ApiResponse<List<BindingDetailResponse>> getBindingsByCrewId(@PathVariable Long crewId) {
        return ApiResponse.success(bindingService.getBindingsByCrewId(crewId));
    }
    
    @GetMapping("/crew/name/{crewName}")
    public ApiResponse<List<BindingDetailResponse>> getBindingsByCrewName(@PathVariable String crewName) {
        return ApiResponse.success(bindingService.getBindingsByCrewName(crewName));
    }
    
    @GetMapping("/date-range")
    public ApiResponse<List<BindingDetailResponse>> getBindingsInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(bindingService.getBindingsInDateRange(startDate, endDate));
    }
    
    @GetMapping("/conflict-check")
    public ApiResponse<ConflictCheckResponse> checkConflict(
            @RequestParam Long propId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long excludeBindingId) {
        return ApiResponse.success(bindingService.checkConflict(propId, startDate, endDate,
                excludeBindingId != null ? excludeBindingId : -1L));
    }
    
    @PutMapping
    public ApiResponse<BindingDetailResponse> updateBinding(@Valid @RequestBody BindingUpdateRequest request) {
        return ApiResponse.success(bindingService.updateBinding(request));
    }
    
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelBinding(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        bindingService.cancelBinding(id, reason, operator);
        return ApiResponse.success("绑定已取消", null);
    }
    
    @GetMapping("/logs")
    public ApiResponse<List<ScheduleChangeLog>> getAllChangeLogs() {
        return ApiResponse.success(bindingService.getAllChangeLogs());
    }
    
    @GetMapping("/logs/conflicts")
    public ApiResponse<List<ScheduleChangeLog>> getChangeLogsWithConflicts() {
        return ApiResponse.success(bindingService.getChangeLogsWithConflicts());
    }
    
    @GetMapping("/logs/binding/{bindingId}")
    public ApiResponse<List<ScheduleChangeLog>> getChangeLogsByBindingId(@PathVariable Long bindingId) {
        return ApiResponse.success(bindingService.getChangeLogsByBindingId(bindingId));
    }
}
