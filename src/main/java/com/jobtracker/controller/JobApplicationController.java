package com.jobtracker.controller;

import com.jobtracker.dto.JobApplicationDto;
import com.jobtracker.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Job Applications", description = "CRUD + analytics + export")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PostMapping
    @Operation(summary = "Create a new job application")
    public ResponseEntity<JobApplicationDto.Response> create(
            @Valid @RequestBody JobApplicationDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.create(request));
    }

    @GetMapping
    @Operation(summary = "Get all applications with optional search, filter, and pagination")
    public ResponseEntity<JobApplicationDto.PagedResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(jobApplicationService.findAll(search, status, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single application by ID")
    public ResponseEntity<JobApplicationDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobApplicationService.findById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an application")
    public ResponseEntity<JobApplicationDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationDto.UpdateRequest request) {
        return ResponseEntity.ok(jobApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an application")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get application analytics and stats")
    public ResponseEntity<JobApplicationDto.AnalyticsResponse> analytics() {
        return ResponseEntity.ok(jobApplicationService.getAnalytics());
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export all applications as CSV")
    public ResponseEntity<byte[]> exportCsv() throws IOException {
        String csv = jobApplicationService.exportToCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "job-applications.csv");
        return ResponseEntity.ok().headers(headers).body(csv.getBytes());
    }
}
