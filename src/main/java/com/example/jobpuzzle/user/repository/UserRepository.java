package com.example.jobpuzzle.user.repository;

import com.example.jobpuzzle.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySocialProviderAndSocialId(String socialProvider, String socialId);
}
