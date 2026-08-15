package com.joe.taskira.project.controller;

import com.joe.taskira.common.web.ApiVersion;
import com.joe.taskira.project.dto.AddProjectMemberRequest;
import com.joe.taskira.project.dto.CreateProjectRequest;
import com.joe.taskira.project.dto.ProjectMemberResponse;
import com.joe.taskira.project.dto.ProjectResponse;
import com.joe.taskira.project.dto.ProjectSummaryResponse;
import com.joe.taskira.project.dto.UpdateProjectRequest;
import com.joe.taskira.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @GetMapping
    public List<ProjectSummaryResponse> listAccessibleProjects() {
        return projectService.listAccessibleProjects();
    }

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @GetMapping("/{id}/members")
    public List<ProjectMemberResponse> listMembers(@PathVariable Long id) {
        return projectService.listMembers(id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        return projectService.addMember(id, request);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        projectService.removeMember(id, userId);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.updateProject(id, request);
    }

    @PatchMapping("/{id}/archive")
    public ProjectResponse archiveProject(@PathVariable Long id) {
        return projectService.archiveProject(id);
    }
}