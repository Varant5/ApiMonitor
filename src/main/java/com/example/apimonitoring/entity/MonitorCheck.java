
package com.example.apimonitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class MonitorCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(nullable = false)
    private String status;

    private Integer responseTimeMs;

    private Integer httpStatusCode;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    public void onCreate() {
        this.checkedAt = LocalDateTime.now();
    }

    public MonitorCheck() {
    }
}
