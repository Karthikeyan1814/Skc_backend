package skc.client.project.SKC_Backend_Files.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


@Component
public class JwtUtil {

	private final String SECRET="heythisissrikathiravanandcoauditingportal";
	
	Key key=Keys.hmacShaKeyFor(SECRET.getBytes());
	
	public String generateToken(String username , String role,Long id) {
		return Jwts.builder().setSubject(username).claim("ROLE",role).claim("ID",id).setIssuedAt( new Date ()).setExpiration( new Date(System.currentTimeMillis()+ 24 * 60 * 60 * 1000))
				.signWith(key,SignatureAlgorithm.HS256).compact();
		
	}
	
	public String extractUsername(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
	}
	
	public String extractRole(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("ROLE",String.class);
	}
	
	public Long extractId(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("ID",Long.class);
	}
	
	public boolean validateToken(
	        String token,
	        UserDetails userDetails) {

	    String username =
	            extractUsername(token);

	    return username.equals(
	            userDetails.getUsername())
	            &&
	            !isTokenExpired(token);
	}
	
	private boolean isTokenExpired(
	        String token) {

	    Date expiration =
	            Jwts.parserBuilder()
	                    .setSigningKey(key)
	                    .build()
	                    .parseClaimsJws(token)
	                    .getBody()
	                    .getExpiration();

	    return expiration.before(
	            new Date());
	}
	
	
}
