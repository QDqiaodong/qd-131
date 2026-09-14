package com.example.filmprop.controller;

import com.example.filmprop.dto.request.PropCreateRequest;
import com.example.filmprop.dto.response.ApiResponse;
import com.example.filmprop.dto.response.BarcodeCheckResponse;
import com.example.filmprop.entity.Prop;
import com.example.filmprop.service.PropService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/props")
public class PropController {
    private final PropService propService;
    
    public PropController(PropService propService) {
        this.propService = propService;
    }
    
    @PostMapping
    public ApiResponse<Prop> createProp(@Valid @RequestBody PropCreateRequest request) {
        return ApiResponse.success(propService.createProp(request));
    }
    
    @GetMapping
    public ApiResponse<List<Prop>> getAllProps(
            @RequestParam(required = false) String sceneType,
            @RequestParam(required = false) String status) {
        if (sceneType != null) {
            return ApiResponse.success(propService.getPropsBySceneType(sceneType));
        }
        if (status != null) {
            return ApiResponse.success(propService.getPropsByStatus(status));
        }
        return ApiResponse.success(propService.getAllProps());
    }
    
    @GetMapping("/{id}")
    public ApiResponse<Prop> getPropById(@PathVariable Long id) {
        return ApiResponse.success(propService.getPropById(id));
    }
    
    @GetMapping("/code/{propCode}")
    public ApiResponse<Prop> getPropByCode(@PathVariable String propCode) {
        return ApiResponse.success(propService.getPropByCode(propCode));
    }

    @GetMapping("/barcode-check")
    public ApiResponse<BarcodeCheckResponse> checkBarcode(@RequestParam String code) {
        return ApiResponse.success(propService.checkBarcode(code));
    }
    
    @PutMapping("/{id}")
    public ApiResponse<Prop> updateProp(@PathVariable Long id, @Valid @RequestBody PropCreateRequest request) {
        return ApiResponse.success(propService.updateProp(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProp(@PathVariable Long id) {
        propService.deleteProp(id);
        return ApiResponse.success(null);
    }
}
