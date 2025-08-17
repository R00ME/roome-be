package com.roome.domain.apiUsage.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user_api_usage")
public class UserApiUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String domain;
    private String apiUri;
    private LocalDate date;
    private Long count;

    @Builder
    public UserApiUsage(Long userId, String domain, String apiUri, LocalDate date, Long count) {
        this.userId = userId;
        this.domain = domain;
        this.apiUri = apiUri;
        this.date = date;
        this.count = count;
    }
}
