package com.example.apimonitoring.repository;

import com.example.apimonitoring.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    Optional<Monitor> findByUrl(String url);

    boolean existsByUrl(String url);

    List<Monitor> findAllByOwnerIdOrderByIdAsc(Long ownerId);

    Optional<Monitor> findByIdAndOwnerId(Long id, Long ownerId);
}
