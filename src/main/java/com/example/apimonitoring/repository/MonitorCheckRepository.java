package com.example.apimonitoring.repository;

import com.example.apimonitoring.entity.MonitorCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, Long> {
}
