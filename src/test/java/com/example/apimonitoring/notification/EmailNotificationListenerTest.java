package com.example.apimonitoring.notification;

import com.example.apimonitoring.entity.User;
import com.example.apimonitoring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationListenerTest {

    private static final String FROM = "alerts@api-monitoring.local";
    private static final LocalDateTime CHECKED_AT = LocalDateTime.of(2026, 11, 17, 14, 30, 5);

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    private EmailNotificationListener listener;

    @BeforeEach
    void setUp() {
        // called directly, so @Async does not apply here — the method runs synchronously
        listener = new EmailNotificationListener(mailSender, userRepository, FROM);
    }

    @Test
    void shouldSendDownEmailToMonitorOwner() {

        givenOwner(7L, "alice@example.com");

        listener.onStatusChanged(event("UP", "DOWN", 503, "Service Unavailable"));

        SimpleMailMessage message = sentMessage();
        assertThat(message.getFrom()).isEqualTo(FROM);
        assertThat(message.getTo()).containsExactly("alice@example.com");
        assertThat(message.getSubject()).isEqualTo("[DOWN] GitHub API is not responding");
        assertThat(message.getText())
                .contains("https://api.github.com")
                .contains("HTTP status: 503")
                .contains("Service Unavailable")
                .contains("2026-11-17 14:30:05");
    }

    @Test
    void shouldDescribeTimeoutWithoutHttpStatus() {

        givenOwner(7L, "alice@example.com");

        listener.onStatusChanged(event("UP", "DOWN", null, "Read timed out"));

        assertThat(sentMessage().getText())
                .contains("HTTP status: no response")
                .contains("Read timed out");
    }

    @Test
    void shouldSendRecoveryEmail() {

        givenOwner(7L, "alice@example.com");

        listener.onStatusChanged(event("DOWN", "UP", 200, null));

        SimpleMailMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo("[RECOVERED] GitHub API is back up");
        assertThat(message.getText()).contains("UP (HTTP 200)");
    }

    @Test
    void shouldSkipWhenOwnerNoLongerExists() {

        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        listener.onStatusChanged(event("UP", "DOWN", 500, "error"));

        verifyNoInteractions(mailSender);
    }

    @Test
    void mailServerFailureShouldNotPropagate() {

        givenOwner(7L, "alice@example.com");
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // monitoring must keep working even if emails cannot be sent
        assertThatCode(() -> listener.onStatusChanged(event("UP", "DOWN", 500, "error")))
                .doesNotThrowAnyException();
    }

    // ---------- helpers ----------

    private void givenOwner(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
    }

    private MonitorStatusChangedEvent event(String previous, String current, Integer httpStatus, String error) {
        return new MonitorStatusChangedEvent(
                1L, "GitHub API", "https://api.github.com", 7L,
                previous, current, httpStatus, error, CHECKED_AT
        );
    }

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
