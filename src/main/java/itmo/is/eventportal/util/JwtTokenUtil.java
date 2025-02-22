package itmo.is.eventportal.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.expiration}")
	private Long jwtExpiration;

	private String getEncodedSecret() {
		return Base64.getEncoder().encodeToString(jwtSecret.getBytes());
	}

	public String generateJwtToken(String email) {
		return Jwts.builder()
				.setSubject(email)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
				.signWith(SignatureAlgorithm.HS512, getEncodedSecret())
				.compact();
	}

	public boolean validateJwtToken(String token) {
		try {
			Jwts.parser().setSigningKey(getEncodedSecret()).parseClaimsJws(token);
			return true;
		} catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
			ex.printStackTrace();
		}
		return false;
	}

	public String getEmailFromJwtToken(String token) {
		try {
			return Jwts.parser().setSigningKey(getEncodedSecret()).parseClaimsJws(token).getBody().getSubject();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}