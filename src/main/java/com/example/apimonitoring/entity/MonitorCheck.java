package com.example.apimonitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@Setter
public class MonitorCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Monitor monitor;

    @Column(nullable = false)
    private String status;

    private Integer responseTimeMs;

    private Integer httpStatusCode;

    @Column(nullable = false)
    private Instant checkedAt;

    @PrePersist
    public void onCreate() {
        this.checkedAt = Instant.now();
    }

    public MonitorCheck() {
    }
}
