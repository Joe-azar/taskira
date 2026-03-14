package com.joe.taskira.project.repository;

import com.joe.taskira.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
            select distinct p
            from Project p
            left join ProjectMember pm on pm.project = p
            where p.owner.id = :userId
               or pm.user.id = :userId
            order by p.name asc
            """)
    List<Project> findAccessibleProjects(Long userId);
}