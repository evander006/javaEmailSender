package com.example.emailsender.provider;

import com.example.emailsender.exception.BusinessException;
import com.example.emailsender.service.EmailService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailProvider implements EmailService {
    private static final String CIRCUIT_BREAKER_NAME = "myService";

    private final JavaMailSender javaMailSender;
    private final String mailSender;
    private final CircuitBreaker circuitBreaker;

    public EmailProvider(JavaMailSender javaMailSender,
                         @Value("${spring.mail.sender}") String mailSender,
                         CircuitBreakerRegistry circuitBreakerRegistry) {
        this.javaMailSender = javaMailSender;
        this.mailSender = mailSender;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(fallbackMethod = "fallbackSendSignedPdf", name = CIRCUIT_BREAKER_NAME)
    @Override
    public void sendSignedPdf(String recipientEmail, String subject, String messageText, String fileName, byte[] pdf) {
        final var message = javaMailSender.createMimeMessage();
        try {
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailSender);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(messageText);
            final String attachmentName = fileName == null || fileName.isBlank() ? "document.pdf" : fileName;
            helper.addAttachment(attachmentName, new ByteArrayResource(pdf), "application/pdf");
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new BusinessException("Ошибка отправки email-сообщения: " + e.getMessage(), e);
        }
    }

    public void fallbackSendSignedPdf(String recipientEmail,
                                      String subject,
                                      String messageText,
                                      String fileName,
                                      byte[] pdf,
                                      Throwable throwable) {
        log.warn("Использован fallback для sendSignedPdf, состояние - [{}]. Причина: {}",
                circuitBreaker.getState(),
                throwable.getMessage());
        final String errMsg = CircuitBreaker.State.OPEN.equals(circuitBreaker.getState())
                ? "Почтовый сервис временно недоступен. Мы работаем над устранением ошибки"
                : "Ошибка отправки email-сообщения: " + throwable.getMessage();
        throw new BusinessException(errMsg, throwable);
    }

    @Override
    public void fallbackEmail(String recipientEmail, String subject, String messageText, Throwable throwable) {
        log.warn("Использован fallback для sendEmail, состояние - [{}]. Причина: {}",
                circuitBreaker.getState(),
                throwable.getMessage());
        final String errMsg = CircuitBreaker.State.OPEN.equals(circuitBreaker.getState()) ?
                "Почтовый сервис временно недоступен. Мы работаем над устранением ошибки" :
                "Ошибка отправки email-сообщения";
        throw new BusinessException(errMsg);
    }
}
