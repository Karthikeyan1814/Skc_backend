package skc.client.project.SKC_Backend_Files.Config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import skc.client.project.SKC_Backend_Files.Security.JwtFilter;

@Configuration
public class SecurityConfig {

	@Autowired
	private JwtFilter jwtfilter;
	
	@Bean
	SecurityFilterChain securityfilterchain(HttpSecurity http) {
		return http.csrf(csrf-> csrf.disable())
				.authorizeHttpRequests(auth ->{
					auth.requestMatchers("/login","/skc/register").permitAll();
					
					auth.requestMatchers("/Admin/**").hasRole("ADMIN");
					auth.anyRequest().authenticated();
				})
				.addFilterBefore(jwtfilter,UsernamePasswordAuthenticationFilter.class)
				.build();
	}
	@Bean
	PasswordEncoder passwordencoder() {
		return new BCryptPasswordEncoder();
	}
}
