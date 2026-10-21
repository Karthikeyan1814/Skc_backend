package skc.client.project.SKC_Backend_Files.Security;

import java.io.IOException;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import skc.client.project.SKC_Backend_Files.Service.CustomeUserDetailService;

@Component
public class JwtFilter  extends GenericFilter{

	@Autowired
	private JwtUtil jwtutil;
	
	@Autowired
	private CustomeUserDetailService UserServices;
	
	@Override
	public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req=(HttpServletRequest) request;
		
		String authHeader=req.getHeader("Authorization");
		
		if(authHeader != null && authHeader.startsWith("Bearer ")) {
			
			System.out.println("Before try");
			
			try {
				
				String token = authHeader.substring(7);
				String username = jwtutil.extractUsername(token);
				String role = jwtutil.extractRole(token);
				Long id = jwtutil.extractId(token);

				UserDetails user;
				if (role != null && id != null) {
					user = new skc.client.project.SKC_Backend_Files.Wrapper.AuthUser(id, username, "", role);
				} else {
					user = UserServices.loadUserByUsername(username);
				}

				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
						user,
						null,
						user.getAuthorities());

				SecurityContextHolder.getContext().setAuthentication(auth);
				System.out.print(user.getUsername());
				System.out.print(user.getAuthorities());
				System.out.print("Token"+token);
				
				
			} catch(io.jsonwebtoken.ExpiredJwtException eje) {
				System.out.println("Expired JWT Token: " + eje.getMessage());
				if (response instanceof jakarta.servlet.http.HttpServletResponse httpResp) {
					httpResp.setHeader("X-Token-Expired", "true");
				}
			} catch(Exception e) {
				System.out.println("Invalid Token: " + e.getMessage());
			}
			
		}
		chain.doFilter(request, response);
	}
}
