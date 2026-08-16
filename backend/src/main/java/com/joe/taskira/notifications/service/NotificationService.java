package com.joe.taskira.notifications.service;

import com.joe.taskira.comment.event.CommentCreatedEvent;
import com.joe.taskira.ticket.event.TicketAssignedEvent;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AFTER_COMMIT, not a plain @EventListener: a ticket assignment or comment whose
 * transaction later rolls back must never trigger an email for something that, from
 * the database's point of view, never actually happened. Delivery is best-effort - any
 * MailException is logged and swallowed, never propagated, so a Mailpit hiccup can
 * never fail the ticket/comment operation that triggered it. See ADR-0020.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.notifications.from-address}")
    private String fromAddress;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketAssigned(TicketAssignedEvent event) {
        userRepository.findById(event.assigneeId())
                .filter(User::isActive)
                .ifPresent(assignee -> send(
                        assignee.getEmail(),
                        "[Taskira] Ticket assigned: " + event.ticketReference(),
                        "You have been assigned to ticket " + event.ticketReference() + ": " + event.ticketTitle()
                ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        for (Long recipientId : event.recipientUserIds()) {
            userRepository.findById(recipientId)
                    .filter(User::isActive)
                    .ifPresent(recipient -> send(
                            recipient.getEmail(),
                            "[Taskira] New comment on " + event.ticketReference(),
                            "A new comment was added to ticket " + event.ticketReference() + ": " + event.ticketTitle()
                    ));
        }
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send notification email to {}: {}", to, ex.getMessage());
        }
    }
}
