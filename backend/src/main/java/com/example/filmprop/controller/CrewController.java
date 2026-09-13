package com.example.filmprop.controller;

import com.example.filmprop.dto.request.CrewCreateRequest;
import com.example.filmprop.dto.response.ApiResponse;
import com.example.filmprop.entity.Crew;
import com.example.filmprop.service.CrewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crews")
public class CrewController {
    private final CrewService crewService;
    
    public CrewController(CrewService crewService) {
        this.crewService = crewService;
    }
    
    @PostMapping
    public ApiResponse<Crew> createCrew(@Valid @RequestBody CrewCreateRequest request) {
        return ApiResponse.success(crewService.createCrew(request));
    }
    
    @GetMapping
    public ApiResponse<List<Crew>> getAllCrews(@RequestParam(required = false) String status) {
        if (status != null) {
            return ApiResponse.success(crewService.getCrewsByStatus(status));
        }
        return ApiResponse.success(crewService.getAllCrews());
    }
    
    @GetMapping("/{id}")
    public ApiResponse<Crew> getCrewById(@PathVariable Long id) {
        return ApiResponse.success(crewService.getCrewById(id));
    }
    
    @GetMapping("/name/{crewName}")
    public ApiResponse<Crew> getCrewByName(@PathVariable String crewName) {
        return ApiResponse.success(crewService.getCrewByName(crewName));
    }
    
    @PutMapping("/{id}")
    public ApiResponse<Crew> updateCrew(@PathVariable Long id, @Valid @RequestBody CrewCreateRequest request) {
        return ApiResponse.success(crewService.updateCrew(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCrew(@PathVariable Long id) {
        crewService.deleteCrew(id);
        return ApiResponse.success(null);
    }
}
