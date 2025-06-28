package com.roome.domain.rank.entity;

import com.roome.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
@Getter
@Setter
@NoArgsConstructor
public class UserActivity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ActivityType activityType;

	@Column(nullable = false)
	private int score;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	// 관련 엔티티 id (도서, 음악, 방명록 등)
	private Long relatedEntityId;

}
