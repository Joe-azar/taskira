package com.joe.taskira.user.repository;

import com.joe.taskira.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByActiveTrueOrderByFirstNameAscLastNameAsc();

    @Query("""
            select u
            from User u
            where u.active = true
              and (
                    lower(u.firstName) like lower(concat('%', :search, '%'))
                 or lower(u.lastName) like lower(concat('%', :search, '%'))
                 or lower(u.email) like lower(concat('%', :search, '%'))
              )
            order by u.firstName asc, u.lastName asc
            """)
    List<User> searchActiveUsers(String search);
}