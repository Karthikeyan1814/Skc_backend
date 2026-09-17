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
				
				String token =authHeader.substring(7);
				
				String username= jwtutil.extractUsername(token);
				
				
				System.out.println("before user");
				UserDetails user=UserServices.loadUserByUsername(username);
				System.out.println("After user");
				
				UsernamePasswordAuthenticationToken auth= new UsernamePasswordAuthenticationToken(
						user,
						null,
						user.getAuthorities());
				
				SecurityContextHolder.getContext().setAuthentication(auth);
				System.out.print(token);
				
				
			}catch(Exception e) {
				System.out.println("Invalid Token");
			}
			
		}
		chain.doFilter(request, response);
	}
}
