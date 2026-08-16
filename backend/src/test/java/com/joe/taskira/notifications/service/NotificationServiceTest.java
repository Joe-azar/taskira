package com.joe.taskira.notifications.service;

import com.joe.taskira.comment.event.CommentCreatedEvent;
import com.joe.taskira.ticket.event.TicketAssignedEvent;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    private User activeUser(long id, String email) {
        return User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .passwordHash("encoded")
                .globalRole(GlobalRole.USER)
                .active(true)
                .build();
    }

    private NotificationService service() {
        NotificationService service = new NotificationService(mailSender, userRepository);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@taskira.test");
        return service;
    }

    @Test
    void ticketAssignedSendsAnEmailToTheActiveAssignee() {
        notificationService = service();
        User assignee = activeUser(1L, "assignee@taskira.test");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(assignee));

        notificationService.onTicketAssigned(new TicketAssignedEvent(10L, "PROJ-1", "Fix the thing", 1L, 2L));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("assignee@taskira.test");
        assertThat(message.getSubject()).contains("PROJ-1");
        assertThat(message.getText()).contains("PROJ-1").contains("Fix the thing");
    }

    @Test
    void ticketAssignedSkipsAnInactiveAssignee() {
        notificationService = service();
        User inactiveAssignee = activeUser(1L, "gone@taskira.test");
        inactiveAssignee.setActive(false);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(inactiveAssignee));

        notificationService.onTicketAssigned(new TicketAssignedEvent(10L, "PROJ-1", "Fix the thing", 1L, 2L));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void ticketAssignedSkipsAnAssigneeThatNoLongerExists() {
        notificationService = service();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        notificationService.onTicketAssigned(new TicketAssignedEvent(10L, "PROJ-1", "Fix the thing", 1L, 2L));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void commentCreatedNotifiesEveryActiveRecipient() {
        notificationService = service();
        User creator = activeUser(1L, "creator@taskira.test");
        User assignee = activeUser(2L, "assignee@taskira.test");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(assignee));

        notificationService.onCommentCreated(new CommentCreatedEvent(5L, 10L, "PROJ-1", "Fix the thing", 3L, Set.of(1L, 2L)));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(m -> m.getTo()[0])
                .containsExactlyInAnyOrder("creator@taskira.test", "assignee@taskira.test");
    }

    @Test
    void mailSendFailureIsLoggedAndSwallowedNotPropagated() {
        notificationService = service();
        User assignee = activeUser(1L, "assignee@taskira.test");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(assignee));
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.onTicketAssigned(new TicketAssignedEvent(10L, "PROJ-1", "Fix the thing", 1L, 2L));
        // No exception propagated - the assertion is simply that this line is reached.
    }
}
