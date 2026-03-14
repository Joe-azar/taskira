package com.joe.taskira.project.service;

import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.project.dto.AddProjectMemberRequest;
import com.joe.taskira.project.dto.CreateProjectRequest;
import com.joe.taskira.project.dto.ProjectMemberResponse;
import com.joe.taskira.project.dto.ProjectResponse;
import com.joe.taskira.project.dto.ProjectSummaryResponse;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectResponse createProject(CreateProjectRequest request) {
        User currentUser = SecurityUtils.getCurrentUser().getUser();
        String normalizedCode = normalizeCode(request.code());

        if (projectRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ConflictException("Project code is already in use");
        }

        Project project = Project.builder()
                .code(normalizedCode)
                .name(request.name().trim())
                .description(normalizeDescription(request.description()))
                .owner(currentUser)
                .status(ProjectStatus.ACTIVE)
                .ticketSequence(0)
                .build();

        project = projectRepository.save(project);

        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(currentUser)
                .projectRole(ProjectRole.OWNER)
                .build();

        projectMemberRepository.save(ownerMember);

        return ProjectResponse.from(project, 1);
    }

    @Transactional
    public List<ProjectSummaryResponse> listAccessibleProjects() {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        List<Project> projects = currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN
                ? projectRepository.findAll().stream().sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).toList()
                : projectRepository.findAccessibleProjects(currentUser.getId());

        return projects.stream()
                .map(ProjectSummaryResponse::from)
                .toList();
    }

    @Transactional
    public ProjectResponse getProjectById(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        assertCanAccessProject(project);

        long memberCount = projectMemberRepository.countByProjectId(projectId);
        return ProjectResponse.from(project, memberCount);
    }

    public ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request) {
        Project project = findProjectOrThrow(projectId);
        assertCanManageProject(project);

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!targetUser.isActive()) {
            throw new ConflictException("Inactive user cannot be added to a project");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.userId())) {
            throw new ConflictException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(targetUser)
                .projectRole(request.projectRole())
                .build();

        member = projectMemberRepository.save(member);

        return ProjectMemberResponse.from(member);
    }

    @Transactional
    public List<ProjectMemberResponse> listMembers(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        assertCanAccessProject(project);

        return projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(ProjectMemberResponse::from)
                .toList();
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private void assertCanAccessProject(Project project) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        boolean isOwner = project.getOwner().getId().equals(currentUser.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId());

        if (!isOwner && !isMember) {
            throw new ForbiddenException("You are not allowed to access this project");
        }
    }

    private void assertCanManageProject(Project project) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        if (project.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not allowed to manage this project"));

        if (membership.getProjectRole() != ProjectRole.OWNER && membership.getProjectRole() != ProjectRole.MANAGER) {
            throw new ForbiddenException("You are not allowed to manage this project");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}