package com.joe.taskira.project.controller;

import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.GlobalExceptionHandler;
import com.joe.taskira.project.dto.ProjectResponse;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.service.ProjectService;
import com.joe.taskira.security.config.RestAccessDeniedHandler;
import com.joe.taskira.security.config.RestAuthenticationEntryPoint;
import com.joe.taskira.security.config.SecurityConfig;
import com.joe.taskira.security.service.CustomUserDetailsService;
import com.joe.taskira.user.dto.UserSummaryResponse;
import com.joe.taskira.user.enums.GlobalRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class ProjectControllerTest {

    private static final String PROJECTS_URL = "/api/v1/projects";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listProjectsRequiresAuthentication() throws Exception {
        mockMvc.perform(get(PROJECTS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value(PROJECTS_URL));

        verifyNoInteractions(projectService);
    }

    @Test
    @WithMockUser(username = "owner@taskira.test")
    void createProjectReturnsCreatedForAValidAuthenticatedRequest() throws Exception {
        when(projectService.createProject(any())).thenReturn(projectResponse());

        mockMvc.perform(post(PROJECTS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "TASK",
                                  "name": "Taskira",
                                  "description": "Enterprise training project"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.code").value("TASK"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.owner.email").value("owner@taskira.test"))
                .andExpect(jsonPath("$.memberCount").value(1));

        verify(projectService).createProject(any());
    }

    @Test
    @WithMockUser(username = "owner@taskira.test")
    void createProjectReturnsStructuredBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post(PROJECTS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "X",
                                  "name": "Taskira"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Project code must contain between 2 and 20 characters"))
                .andExpect(jsonPath("$.instance").value(PROJECTS_URL));

        verifyNoInteractions(projectService);
    }

    @Test
    @WithMockUser(username = "owner@taskira.test")
    void archiveProjectMapsBusinessAuthorizationFailureToForbidden() throws Exception {
        when(projectService.archiveProject(42L))
                .thenThrow(new ForbiddenException("You are not allowed to manage this project"));

        mockMvc.perform(patch(PROJECTS_URL + "/42/archive").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You are not allowed to manage this project"))
                .andExpect(jsonPath("$.instance").value(PROJECTS_URL + "/42/archive"));
    }

    private ProjectResponse projectResponse() {
        Instant timestamp = Instant.parse("2026-08-14T10:00:00Z");
        UserSummaryResponse owner = new UserSummaryResponse(
                7L,
                "Project Owner",
                "owner@taskira.test",
                GlobalRole.USER,
                true
        );
        return new ProjectResponse(
                42L,
                "TASK",
                "Taskira",
                "Enterprise training project",
                ProjectStatus.ACTIVE,
                owner,
                1L,
                timestamp,
                timestamp
        );
    }
}
