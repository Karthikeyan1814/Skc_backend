package skc.client.project.SKC_Backend_Files.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
public class JwtUtil {

	private final String SECRET="heythisissrikathiravanandco";
	
	Key key=Keys.hmacShaKeyFor(SECRET.getBytes());
	
	public String generateToken(String username , String role) {
		return Jwts.builder().setSubject(username).claim("role",role).setIssuedAt( new Date ()).setExpiration( new Date(System.currentTimeMillis()+48700000))
				.signWith(key,SignatureAlgorithm.HS256).compact();
		
	}
	
	public String extractUsername(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
	}
	
	public String extractRole(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role",String.class);
	}
	
}
