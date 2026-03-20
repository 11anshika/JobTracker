package com.jobtracker.service;

import com.jobtracker.dto.JobApplicationDto;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.JobApplication.Status;
import com.jobtracker.entity.User;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public JobApplicationDto.Response create(JobApplicationDto.CreateRequest request) {
        User user = currentUser();
        JobApplication app = JobApplication.builder()
            .user(user)
            .companyName(request.getCompanyName())
            .jobTitle(request.getJobTitle())
            .appliedDate(request.getAppliedDate())
            .status(request.getStatus() != null ? request.getStatus() : Status.APPLIED)
            .jobUrl(request.getJobUrl())
            .location(request.getLocation())
            .salary(request.getSalary())
            .notes(request.getNotes())
            .interviewDate(request.getInterviewDate())
            .build();

        return toResponse(jobApplicationRepository.save(app));
    }

    @Transactional(readOnly = true)
    public JobApplicationDto.PagedResponse findAll(String search, String status, int page, int size, String sortBy, String sortDir) {
        User user = currentUser();
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = Status.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        Page<JobApplication> pageResult = jobApplicationRepository.findByUserWithFilters(
            user,
            (search != null && !search.isBlank()) ? search : null,
            statusEnum,
            pageable
        );

        return JobApplicationDto.PagedResponse.builder()
            .content(pageResult.getContent().stream().map(this::toResponse).toList())
            .page(pageResult.getNumber())
            .size(pageResult.getSize())
            .totalElements(pageResult.getTotalElements())
            .totalPages(pageResult.getTotalPages())
            .last(pageResult.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public JobApplicationDto.Response findById(Long id) {
        User user = currentUser();
        return toResponse(findByIdAndUser(id, user));
    }

    @Transactional
    public JobApplicationDto.Response update(Long id, JobApplicationDto.UpdateRequest request) {
        User user = currentUser();
        JobApplication app = findByIdAndUser(id, user);

        if (request.getCompanyName() != null) app.setCompanyName(request.getCompanyName());
        if (request.getJobTitle() != null)   app.setJobTitle(request.getJobTitle());
        if (request.getAppliedDate() != null) app.setAppliedDate(request.getAppliedDate());
        if (request.getStatus() != null)     app.setStatus(request.getStatus());
        if (request.getJobUrl() != null)     app.setJobUrl(request.getJobUrl());
        if (request.getLocation() != null)   app.setLocation(request.getLocation());
        if (request.getSalary() != null)     app.setSalary(request.getSalary());
        if (request.getNotes() != null)      app.setNotes(request.getNotes());
        if (request.getInterviewDate() != null) app.setInterviewDate(request.getInterviewDate());

        return toResponse(jobApplicationRepository.save(app));
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUser();
        JobApplication app = findByIdAndUser(id, user);
        jobApplicationRepository.delete(app);
    }

    @Transactional(readOnly = true)
    public JobApplicationDto.AnalyticsResponse getAnalytics() {
        User user = currentUser();
        List<Object[]> statusCounts = jobApplicationRepository.countByStatusForUser(user);

        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Status s : Status.values()) countMap.put(s.name(), 0L);
        statusCounts.forEach(row -> countMap.put(((Status) row[0]).name(), (Long) row[1]));

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();
        long interviewed = countMap.getOrDefault("INTERVIEW", 0L) + countMap.getOrDefault("OFFER", 0L);
        double responseRate = total > 0 ? (interviewed * 100.0 / total) : 0;

        LocalDate weekAgo  = LocalDate.now().minusDays(7);
        LocalDate monthAgo = LocalDate.now().minusDays(30);

        List<JobApplication> all = jobApplicationRepository.findByUser(user);
        long thisWeek  = all.stream().filter(a -> !a.getAppliedDate().isBefore(weekAgo)).count();
        long thisMonth = all.stream().filter(a -> !a.getAppliedDate().isBefore(monthAgo)).count();

        return JobApplicationDto.AnalyticsResponse.builder()
            .statusCounts(countMap)
            .totalApplications(total)
            .thisWeek(thisWeek)
            .thisMonth(thisMonth)
            .responseRate(Math.round(responseRate * 10.0) / 10.0)
            .build();
    }

    @Transactional(readOnly = true)
    public String exportToCsv() throws IOException {
        User user = currentUser();
        List<JobApplication> apps = jobApplicationRepository.findByUser(user);

        StringWriter sw = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("ID", "Company", "Job Title", "Applied Date", "Status",
                       "Location", "Salary", "Interview Date", "Job URL", "Notes")
            .build();

        try (CSVPrinter printer = new CSVPrinter(sw, format)) {
            for (JobApplication app : apps) {
                printer.printRecord(
                    app.getId(),
                    app.getCompanyName(),
                    app.getJobTitle(),
                    app.getAppliedDate(),
                    app.getStatus(),
                    app.getLocation(),
                    app.getSalary(),
                    app.getInterviewDate(),
                    app.getJobUrl(),
                    app.getNotes()
                );
            }
        }
        return sw.toString();
    }


    private JobApplication findByIdAndUser(Long id, User user) {
        return jobApplicationRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Job application", id));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private JobApplicationDto.Response toResponse(JobApplication app) {
        return JobApplicationDto.Response.builder()
            .id(app.getId())
            .companyName(app.getCompanyName())
            .jobTitle(app.getJobTitle())
            .appliedDate(app.getAppliedDate())
            .status(app.getStatus())
            .jobUrl(app.getJobUrl())
            .location(app.getLocation())
            .salary(app.getSalary())
            .notes(app.getNotes())
            .interviewDate(app.getInterviewDate())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt())
            .build();
    }
}
