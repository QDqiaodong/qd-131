package com.example.filmprop.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class CrewCreateRequest {
    @NotBlank(message = "剧组名称不能为空")
    private String crewName;
    
    private String projectName;
    
    private String director;

    @NotBlank(message = "剧组片种不能为空")
    private String genre;

    private LocalDate startDate;
    
    private LocalDate endDate;

    public String getCrewName() {
        return crewName;
    }

    public void setCrewName(String crewName) {
        this.crewName = crewName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
