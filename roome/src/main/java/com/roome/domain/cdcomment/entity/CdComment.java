package com.roome.domain.cdcomment.entity;

import com.roome.domain.mycd.entity.MyCd;
import com.roome.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CdComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
	private MyCd myCd;

	private int timestamp;
	private String content;

	@Column(updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public CdComment(Long id, User user, MyCd myCd, int timestamp, String content) {
		this.id = id;
		this.user = user;
		this.myCd = myCd;
		this.timestamp = timestamp;
		this.content = content;
	}

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
}
