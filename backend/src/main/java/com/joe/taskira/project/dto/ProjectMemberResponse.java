package com.joe.taskira.project.dto;

import com.joe.taskira.project.entity.ProjectMember;
import com.joe.taskira.project.enums.ProjectRole;

import java.time.Instant;

public record ProjectMemberResponse(
        Long userId,
        String fullName,
        String email,
        ProjectRole projectRole,
        Instant joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getProjectRole(),
                member.getJoinedAt()
        );
    }
}