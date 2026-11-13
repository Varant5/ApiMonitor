package com.example.apimonitoring.repository;

import com.example.apimonitoring.entity.MonitorCheck;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, Long> {

    Optional<MonitorCheck> findTopByMonitorIdOrderByCheckedAtDesc(Long monitorId);

    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(Long monitorId);

    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(
            Long monitorId,
            Pageable pageable
    );
}
