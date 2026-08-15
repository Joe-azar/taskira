package com.joe.taskira.project.service;

import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.project.dto.CreateProjectRequest;
import com.joe.taskira.project.dto.ProjectResponse;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.entity.ProjectMember;
import com.joe.taskira.project.enums.ProjectRole;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectMemberAssignmentCheck projectMemberAssignmentCheck;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProjectNormalizesInputAndAddsCurrentUserAsOwner() {
        User currentUser = authenticate(7L, GlobalRole.USER);
        CreateProjectRequest request = new CreateProjectRequest(
                "  web  ",
                "  Taskira Web  ",
                "  Frontend workspace  "
        );

        when(projectRepository.existsByCodeIgnoreCase("WEB")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(42L);
            project.setCreatedAt(Instant.parse("2026-08-14T10:00:00Z"));
            project.setUpdatedAt(Instant.parse("2026-08-14T10:00:00Z"));
            return project;
        });

        ProjectResponse response = projectService.createProject(request);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectRepository).save(projectCaptor.capture());
        verify(projectMemberRepository).save(memberCaptor.capture());

        assertThat(projectCaptor.getValue())
                .extracting(Project::getCode, Project::getName, Project::getDescription, Project::getStatus)
                .containsExactly("WEB", "Taskira Web", "Frontend workspace", ProjectStatus.ACTIVE);
        assertThat(projectCaptor.getValue().getOwner()).isSameAs(currentUser);
        assertThat(memberCaptor.getValue().getProject()).isSameAs(projectCaptor.getValue());
        assertThat(memberCaptor.getValue().getUser()).isSameAs(currentUser);
        assertThat(memberCaptor.getValue().getProjectRole()).isEqualTo(ProjectRole.OWNER);
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.memberCount()).isEqualTo(1);
    }

    @Test
    void createProjectRejectsAnExistingCodeBeforeWriting() {
        authenticate(7L, GlobalRole.USER);
        CreateProjectRequest request = new CreateProjectRequest(" api ", "API", null);
        when(projectRepository.existsByCodeIgnoreCase("API")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Project code is already in use");

        verify(projectRepository, never()).save(any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void archiveProjectRejectsAUserWithoutManagementRights() {
        User currentUser = authenticate(7L, GlobalRole.USER);
        Project project = project(42L, user(10L, GlobalRole.USER));
        when(projectRepository.findById(42L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(42L, currentUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.archiveProject(42L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to manage this project");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void archiveProjectAllowsAProjectManager() {
        User currentUser = authenticate(7L, GlobalRole.USER);
        Project project = project(42L, user(10L, GlobalRole.USER));
        ProjectMember membership = ProjectMember.builder()
                .project(project)
                .user(currentUser)
                .projectRole(ProjectRole.MANAGER)
                .build();

        when(projectRepository.findById(42L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(42L, currentUser.getId()))
                .thenReturn(Optional.of(membership));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMemberRepository.countByProjectId(42L)).thenReturn(2L);

        ProjectResponse response = projectService.archiveProject(42L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(response.memberCount()).isEqualTo(2L);
        verify(projectRepository).save(project);
    }

    @Test
    void removeMemberRejectsATargetUserWithAssignedTickets() {
        User owner = authenticate(7L, GlobalRole.USER);
        Project project = project(42L, owner);
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user(20L, GlobalRole.USER))
                .projectRole(ProjectRole.MEMBER)
                .build();

        when(projectRepository.findById(42L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(42L, 20L)).thenReturn(Optional.of(member));
        when(projectMemberAssignmentCheck.countAssignedTickets(42L, 20L)).thenReturn(2L);

        assertThatThrownBy(() -> projectService.removeMember(42L, 20L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot remove member with assigned tickets. Reassign or unassign first.");

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMemberDeletesATargetUserWithoutAssignedTickets() {
        User owner = authenticate(7L, GlobalRole.USER);
        Project project = project(42L, owner);
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user(20L, GlobalRole.USER))
                .projectRole(ProjectRole.MEMBER)
                .build();

        when(projectRepository.findById(42L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(42L, 20L)).thenReturn(Optional.of(member));
        when(projectMemberAssignmentCheck.countAssignedTickets(42L, 20L)).thenReturn(0L);

        projectService.removeMember(42L, 20L);

        verify(projectMemberRepository).delete(member);
    }

    private User authenticate(Long id, GlobalRole role) {
        User user = user(id, role);
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        return user;
    }

    private User user(Long id, GlobalRole role) {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("user." + id + "@taskira.test")
                .passwordHash("encoded-password")
                .globalRole(role)
                .active(true)
                .build();
        user.setId(id);
        return user;
    }

    private Project project(Long id, User owner) {
        Project project = Project.builder()
                .code("TASK")
                .name("Taskira")
                .description("Project management")
                .owner(owner)
                .status(ProjectStatus.ACTIVE)
                .ticketSequence(0)
                .build();
        project.setId(id);
        project.setCreatedAt(Instant.parse("2026-08-14T10:00:00Z"));
        project.setUpdatedAt(Instant.parse("2026-08-14T10:00:00Z"));
        return project;
    }
}
