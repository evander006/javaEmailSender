package com.example.emailsender.controller;

import com.example.emailsender.service.EmailService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String sendSignedPdf(@RequestParam String to,
                                @RequestParam String subject,
                                @RequestParam(defaultValue = "Подписанный PDF во вложении") String text,
                                @RequestPart("file") MultipartFile file) throws IOException {

        emailService.sendSignedPdf(to, subject, text, file.getOriginalFilename(), file.getBytes());

        return "sent";
    }
}