package com.example.demobot.repository;

import com.example.demobot.entity.TgUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TgUserRepository extends JpaRepository<TgUser, Long> {
}
