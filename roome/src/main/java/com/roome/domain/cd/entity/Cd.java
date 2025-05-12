package com.roome.domain.cd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Cd {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String artist;

	@Column(nullable = false)
	private String album;

	@Column(nullable = false)
	private LocalDate releaseDate;

	@Column(nullable = false)
	private String coverUrl;

	@Column(nullable = false)
	private String youtubeUrl;

	private long duration;

	@OneToMany(mappedBy = "cd", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<CdGenre> cdGenres = new ArrayList<>();

	public static Cd create(String title, String artist, String album, LocalDate releaseDate, String coverUrl,
							String youtubeUrl, long duration) {
		return Cd.builder()
				.title(title)
				.artist(artist)
				.album(album)
				.releaseDate(releaseDate)
				.coverUrl(coverUrl)
				.youtubeUrl(youtubeUrl)
				.duration(duration)
				.cdGenres(new ArrayList<>())
				.build();
	}

	public void addGenre(CdGenre cdGenre) {
		this.cdGenres.add(cdGenre);
	}

	public List<String> getGenres() {
		return cdGenres.stream()
				.map(cdGenre -> cdGenre.getGenreType().getName())
				.toList();
	}
}
