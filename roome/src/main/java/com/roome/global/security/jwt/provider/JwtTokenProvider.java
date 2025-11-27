package com.roome.global.security.jwt.provider;

import com.roome.domain.user.entity.User;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.security.jwt.principal.CustomUser;
import com.roome.global.security.jwt.principal.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider implements InitializingBean {
	private static final String AUTHORITIES_KEY = "auth";
	private final UserRepository userRepository;
	private final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
	private final String secret;
	private final String refreshSecret;
	private Key key;
	private Key refreshKey;

	public JwtTokenProvider(
			UserRepository userRepository, @Value("${JWT_SECRET}") String secret,
			@Value("${JWT_REFRESH_SECRET}") String refreshSecret) {
		this.userRepository = userRepository;
		this.secret = secret;
		this.refreshSecret = refreshSecret;
	}

	// 빈이 생성되고 주입을 받은 후에 secret값을 Base64 Decode해서 key 변수에 할당하기 위해
	@Override
	public void afterPropertiesSet() {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.key = Keys.hmacShaKeyFor(keyBytes);

		byte[] refreshKeyBytes = Decoders.BASE64.decode(refreshSecret);
		this.refreshKey = Keys.hmacShaKeyFor(refreshKeyBytes);
	}

	public String createToken(Authentication authentication) {
		String authorities = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));

		// 토큰의 expire 시간을 설정
		long now = (new Date()).getTime();
		long tokenValidityInMilliseconds = 1800000L;
		Date validity = new Date(now + tokenValidityInMilliseconds);

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		Long userId = principal.getId();
		String email = principal.getEmail();

		return Jwts.builder()
				.setSubject(String.valueOf(userId))
				.claim("userId", String.valueOf(userId))
				.claim("email", email)
				.claim(AUTHORITIES_KEY, authorities) // 정보 저장
				.signWith(key, SignatureAlgorithm.HS256) // 사용할 암호화 알고리즘과 , signature 에 들어갈 secret값 세팅
				.setExpiration(validity) // set Expire Time 해당 옵션 안넣으면 expire안함
				.compact();
	}

	public String createRefreshToken(Long userId) {
		Date now = new Date();
		long refreshTokenValidity = 2592000000L;
		Date expiry = new Date(now.getTime() + refreshTokenValidity);

		return Jwts.builder()
				.setSubject(String.valueOf(userId))
				.setIssuedAt(now)
				.setExpiration(expiry)
				.signWith(refreshKey, SignatureAlgorithm.HS256)
				.compact();
	}

	// 토큰으로 클레임을 만들고 이를 이용해 유저 객체를 만들어서 최종적으로 authentication 객체를 리턴
	public Authentication getAuthentication(String token) {
		Claims claims = Jwts
				.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();

		Long userId = Long.valueOf(claims.get("userId", String.class));
		String email = claims.get("email", String.class);

		Collection<? extends GrantedAuthority> authorities =
				Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
						.map(SimpleGrantedAuthority::new)
						.collect(Collectors.toList());

		CustomUser principal = new CustomUser(userId, email, authorities);

		log.info("🧩 Claims authorities: {}", claims.get("auth"));

		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	public Authentication getAuthenticationFromUserId(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));// exception 관리

		Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getUserRole().name()));

		CustomUser principal = new CustomUser(
				user.getId(),
				user.getEmail(),
				authorities
		);

		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	// accessToken 에서 사용자 식별 정보 추출
	public Long getUserIdFromAccessToken(String accessToken) {
		Claims claims = Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(accessToken)
				.getBody();

		return Long.parseLong(claims.getSubject());
	}

	// refreshToken 에서 사용자 식별 정보 추출
	public Long getUserFromRefreshToken(String refreshToken) {
		Claims claims = Jwts.parserBuilder().setSigningKey(refreshKey).build()
				.parseClaimsJws(refreshToken).getBody();
		return Long.parseLong(claims.getSubject());
	}

	public long getExpiration(String token) {
		Claims claims = getClaims(token);
		return claims.getExpiration().getTime() - System.currentTimeMillis();
	}

	// 토큰의 유효성 검증을 수행
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {

			logger.info("잘못된 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {

			logger.info("만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {

			logger.info("지원되지 않는 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {

			logger.info("JWT 토큰이 잘못되었습니다.");
		}
		return false;
	}

	public boolean validateRefreshToken(String refreshToken) {
		try {
			Jwts.parserBuilder().setSigningKey(refreshKey).build().parseClaimsJws(refreshToken);
			return true;
		} catch (Exception e) {
			logger.warn("유효하지 않은 RefreshToken입니다: " + e.getMessage());
			return false;
		}
	}

	public String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer != null && bearer.startsWith("Bearer ")) return bearer.substring(7);

		return null;
	}

	public long getRemainingValidity(String accessToken) {
		Claims claims = Jwts.parserBuilder()
				.setSigningKey(key)  // 시크릿 키 사용
				.build()
				.parseClaimsJws(accessToken)
				.getBody();

		Date expiration = claims.getExpiration(); // 만료 시간
		long now = System.currentTimeMillis();

		return (expiration.getTime() - now) / 1000;
	}

	private Claims getClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public String getEmailFromAccessToken(String token) {
		Claims claims = getClaims(token);
		return claims.get("email", String.class);
	}

	public List<String> getAuthorities(String token) {
		Claims claims = getClaims(token);
		String roles = claims.get("auth", String.class); // AUTHORITIES_KEY
		return Arrays.asList(roles.split(","));
	}

}
