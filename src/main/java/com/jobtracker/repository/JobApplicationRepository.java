package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.JobApplication.Status;
import com.jobtracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByIdAndUser(Long id, User user);

    Page<JobApplication> findByUser(User user, Pageable pageable);

    @Query("""
        SELECT j FROM JobApplication j
        WHERE j.user = :user
        AND (:search IS NULL OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR j.status = :status)
        """)
    Page<JobApplication> findByUserWithFilters(
        @Param("user") User user,
        @Param("search") String search,
        @Param("status") Status status,
        Pageable pageable
    );

    @Query("""
        SELECT j.status AS status, COUNT(j) AS count
        FROM JobApplication j
        WHERE j.user = :user
        GROUP BY j.status
        """)
    List<Object[]> countByStatusForUser(@Param("user") User user);

    @Query("""
        SELECT j FROM JobApplication j
        WHERE j.interviewDate = :date
        AND j.reminderSent = false
        """)
    List<JobApplication> findUpcomingInterviewsNotNotified(@Param("date") LocalDate date);

    List<JobApplication> findByUser(User user);

    @Query("""
        SELECT j FROM JobApplication j
        WHERE j.user = :user
        AND j.appliedDate >= :from
        ORDER BY j.appliedDate ASC
        """)
    List<JobApplication> findByUserSince(
        @Param("user") User user,
        @Param("from") LocalDate from
    );
}
