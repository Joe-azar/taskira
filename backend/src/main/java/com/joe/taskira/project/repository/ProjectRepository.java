package com.joe.taskira.project.repository;

import com.joe.taskira.project.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Project p
            where p.id = :id
            """)
    Optional<Project> findByIdForUpdate(Long id);
}