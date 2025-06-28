package com.roome.domain.furniture.entity;

import com.roome.domain.furniture.exception.BookshelfMaxLevelException;
import com.roome.domain.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "furniture")
public class Furniture {

	private static int BOOKSHELF_MAX_LEVEL = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Room room;

	@Enumerated(EnumType.STRING)
	@Column(name = "furniture_type", nullable = false)
	private FurnitureType furnitureType;

	@Column(name = "is_visible", nullable = false)
	private Boolean isVisible;

	@ColumnDefault(value = "1")
	private int level;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public static List<Furniture> createDefaultFurnitures(Room room, LocalDateTime now) {
		Furniture bookshelf = init(room, FurnitureType.BOOKSHELF, now);
		Furniture cdRack = init(room, FurnitureType.CD_RACK, now);
		return List.of(bookshelf, cdRack);
	}

	public static Furniture init(Room room, FurnitureType furnitureType, LocalDateTime now) {
		Furniture furniture = new Furniture();
		furniture.room = room;
		furniture.furnitureType = furnitureType;
		furniture.isVisible = false;
		furniture.level = 1;
		furniture.createdAt = now;
		furniture.updatedAt = now;
		return furniture;
	}

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public int getMaxCapacity() {
		return FurnitureCapacity.getCapacity(furnitureType, level);
	}

	public void setVisible(Boolean isVisible) {
		this.isVisible = isVisible;
	}

	public void upgradeLevel() {
		if (BOOKSHELF_MAX_LEVEL == level) {
			throw new BookshelfMaxLevelException();
		}
		level++;
	}
}
