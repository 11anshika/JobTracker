package com.jobtracker.service;

import com.jobtracker.entity.JobApplication;
import com.jobtracker.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailReminderService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    // Runs every day at 8 AM
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendInterviewReminders() {
        if (!mailEnabled) {
            log.debug("Email reminders are disabled");
            return;
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<JobApplication> upcoming = jobApplicationRepository
            .findUpcomingInterviewsNotNotified(tomorrow);

        for (JobApplication app : upcoming) {
            try {
                sendReminderEmail(app);
                app.setReminderSent(true);
                jobApplicationRepository.save(app);
                log.info("Reminder sent for application id={}", app.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for application id={}", app.getId(), e);
            }
        }
    }

    private void sendReminderEmail(JobApplication app) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(app.getUser().getEmail());
        message.setSubject("Interview Reminder: " + app.getCompanyName() + " — Tomorrow");
        message.setText("""
            Hi %s,
            
            Just a reminder that you have an interview scheduled for tomorrow!
            
            Company:   %s
            Role:      %s
            Date:      %s
            
            Good luck! 🚀
            
            — Job Tracker
            """.formatted(
                app.getUser().getFullName(),
                app.getCompanyName(),
                app.getJobTitle(),
                app.getInterviewDate()
            )
        );
        mailSender.send(message);
    }
}
