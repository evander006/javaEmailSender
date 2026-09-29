package com.example.emailsender.service;

public interface EmailService {
    void sendSignedPdf(String recipientEmail, String subject, String messageText, String fileName, byte[] pdf);

}
