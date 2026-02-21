package com.Pranav.finance_tracker.email.service;

import com.Pranav.finance_tracker.email.entity.EmailLog;
import com.Pranav.finance_tracker.email.repository.EmailLogRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Async
    public void sendEmail(User user, String subject, String body) {
        EmailLog emailLog = EmailLog.builder()
                .user(user)
                .subject(subject)
                .body(body)
                .sentAt(LocalDateTime.now())
                .build();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            
            emailLog.setSuccess(true);
            log.info("Email sent successfully to {}", user.getEmail());
        } catch (Exception e) {
            emailLog.setSuccess(false);
            emailLog.setErrorMessage(e.getMessage());
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }

        emailLogRepository.save(emailLog);
    }
}
