package skc.client.project.SKC_Backend_Files.Security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtFilter  extends GenericFilter{

	@Autowired
	private JwtUtil jwtutil;
	
	@Override
	public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req=(HttpServletRequest) request;
		
		String authHeader=req.getHeader("Authorization");
		
		if(authHeader != null && authHeader.startsWith("Bearer ")) {
			
			try {
				String token =authHeader.substring(7);
				
				String username= jwtutil.extractUsername(token);
				
				String role=jwtutil.extractRole(token);
				
				UsernamePasswordAuthenticationToken auth= new UsernamePasswordAuthenticationToken(
						username,
						null,
						List.of(
								new SimpleGrantedAuthority("ROLE_"+role))
						);
				
				SecurityContextHolder.getContext().setAuthentication(auth);
				
				
				
			}catch(Exception e) {
				System.out.println("Invalid Token");
			}
			
		}
		chain.doFilter(request, response);
	}
}
