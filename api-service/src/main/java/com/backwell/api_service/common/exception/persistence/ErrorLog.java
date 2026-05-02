package com.backwell.api_service.common.exception.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "error_logs")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "error_logs_seq")
    @SequenceGenerator(
            name = "error_logs_seq",
            sequenceName = "error_logs_seq"
    )
    private Long id;

    private UUID traceId;
    private String exceptionName;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    private UUID userId;

    private String requestUri;
    private String httpMethod;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
