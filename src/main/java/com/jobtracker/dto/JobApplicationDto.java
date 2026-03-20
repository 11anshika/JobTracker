package com.jobtracker.dto;

import com.jobtracker.entity.JobApplication.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class JobApplicationDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Company name is required")
        @Size(max = 150)
        private String companyName;

        @NotBlank(message = "Job title is required")
        @Size(max = 150)
        private String jobTitle;

        @NotNull(message = "Applied date is required")
        private LocalDate appliedDate;

        private Status status;

        @Size(max = 500)
        private String jobUrl;

        @Size(max = 100)
        private String location;

        private Double salary;

        @Size(max = 2000)
        private String notes;

        private LocalDate interviewDate;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 150)
        private String companyName;

        @Size(max = 150)
        private String jobTitle;

        private LocalDate appliedDate;
        private Status status;

        @Size(max = 500)
        private String jobUrl;

        @Size(max = 100)
        private String location;

        private Double salary;

        @Size(max = 2000)
        private String notes;

        private LocalDate interviewDate;
    }

    @Data @Builder
    public static class Response {
        private Long id;
        private String companyName;
        private String jobTitle;
        private LocalDate appliedDate;
        private Status status;
        private String jobUrl;
        private String location;
        private Double salary;
        private String notes;
        private LocalDate interviewDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder
    public static class AnalyticsResponse {
        private Map<String, Long> statusCounts;
        private long totalApplications;
        private long thisWeek;
        private long thisMonth;
        private double responseRate;
    }

    @Data @Builder
    public static class PagedResponse {
        private java.util.List<Response> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
