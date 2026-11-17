package com.example.apimonitoring.notification;

import com.example.apimonitoring.entity.User;
import com.example.apimonitoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Sends an email to the monitor's owner when the monitor goes DOWN or recovers.
 *
 * Runs asynchronously (@Async): a slow or unavailable mail server must never delay the checks.
 * Can be turned off with monitoring.notifications.enabled=false.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "monitoring.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class EmailNotificationListener {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final String fromAddress;

    public EmailNotificationListener(JavaMailSender mailSender,
                                     UserRepository userRepository,
                                     @Value("${monitoring.notifications.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.fromAddress = fromAddress;
    }

    @Async
    @EventListener
    public void onStatusChanged(MonitorStatusChangedEvent event) {

        User owner = userRepository.findById(event.ownerId()).orElse(null);
        if (owner == null) {
            log.warn("Owner {} of monitor {} not found, notification skipped", event.ownerId(), event.monitorId());
            return;
        }

        SimpleMailMessage message = buildMessage(event, owner.getEmail());

        try {
            mailSender.send(message);
            log.info("Sent {} notification for monitor {} to {}",
                    event.currentStatus(), event.monitorId(), owner.getEmail());
        } catch (MailException e) {
            // an email problem must not break monitoring — log and move on
            log.warn("Could not send notification for monitor {}: {}", event.monitorId(), e.getMessage());
        }
    }

    SimpleMailMessage buildMessage(MonitorStatusChangedEvent event, String recipient) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);

        if (event.isRecovery()) {
            message.setSubject("[RECOVERED] " + event.monitorName() + " is back up");
            message.setText("""
                    Good news — your monitor is responding again.

                    Monitor:     %s
                    URL:         %s
                    Status:      UP (HTTP %s)
                    Checked at:  %s
                    """.formatted(
                    event.monitorName(),
                    event.monitorUrl(),
                    event.httpStatusCode(),
                    event.checkedAt().format(TIME_FORMAT)));
        } else {
            message.setSubject("[DOWN] " + event.monitorName() + " is not responding");
            message.setText("""
                    Your monitor has stopped responding correctly.

                    Monitor:     %s
                    URL:         %s
                    Status:      DOWN
                    HTTP status: %s
                    Error:       %s
                    Checked at:  %s

                    You will get another email when it recovers.
                    """.formatted(
                    event.monitorName(),
                    event.monitorUrl(),
                    event.httpStatusCode() != null ? event.httpStatusCode() : "no response",
                    event.errorMessage() != null ? event.errorMessage() : "-",
                    event.checkedAt().format(TIME_FORMAT)));
        }

        return message;
    }
}
