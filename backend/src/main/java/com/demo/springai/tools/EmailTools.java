package com.demo.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class EmailTools {

    public record Email(String id, String from, String subject, String snippet) {}

    private static final List<Email> INBOX = List.of(
            new Email("1", "billing@acme.com", "Invoice #1042 is due",
                    "Your invoice #1042 of $1,200 is due on June 30."),
            new Email("2", "no-reply@github.com", "Security alert",
                    "A new sign-in to your account from a new device."),
            new Email("3", "billing@figma.com", "Receipt for your payment",
                    "Thanks! We received your $15 payment for Figma Pro."),
            new Email("4", "team@notion.so", "Your weekly digest",
                    "Here's what happened in your workspace this week."),
            new Email("5", "ap@company.com", "Invoice approval needed",
                    "Please approve the vendor invoice attached for processing."),
            new Email("6", "newsletter@spring.io", "This Week in Spring",
                    "Spring AI 2.0, Boot 4.1 and more.")
    );

    @Tool(description = "Search the user's email inbox. Returns emails whose sender, subject, or body match the query.")
    public List<Email> searchEmails(
            @ToolParam(description = "keywords to search for, e.g. 'invoice'") String query) {
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return INBOX.stream()
                .filter(email -> haystack(email).contains(needle))
                .toList();
    }

    private static String haystack(Email email) {
        return (email.from() + " " + email.subject() + " " + email.snippet()).toLowerCase(Locale.ROOT);
    }
}
