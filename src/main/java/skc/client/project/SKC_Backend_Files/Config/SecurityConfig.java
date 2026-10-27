package skc.client.project.SKC_Backend_Files.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    SecurityFilterChain securityfilterchain(HttpSecurity http) throws Exception {

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {

                    // Public endpoints
                    auth.requestMatchers(
                            "/error",
                            "/skc/SignIn/**",
                            "/skc/register/**",
                            "/skc/Membership/**",
                            "/Membership/**",
                            "/skc/Public/**",
                            "/Public/**"
                    ).permitAll();

                    // CORS preflight
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // Admin & Staff shared client directory and form fields
                    auth.requestMatchers("/skc/Admin/Clients").hasAnyRole("ADMIN", "STAFF");
                    auth.requestMatchers("/skc/Admin/ClientForm/**").hasAnyRole("ADMIN", "STAFF");

                    // Explicitly safeguard Delete Client endpoint - Strictly Admin only
                    auth.requestMatchers("/skc/Admin/Client/Delete/**", "/skc/Client/Delete/**").hasRole("ADMIN");

                    // Admin-only routes
                    auth.requestMatchers("/skc/Admin/**").hasRole("ADMIN");

                    // Staff routes (admin can also access)
                    auth.requestMatchers("/skc/Staff/**").hasAnyRole("ADMIN", "STAFF");

                    // Client billing routes (Strictly Admin and Client only - Staff completely excluded)
                    auth.requestMatchers("/skc/Client/Billing/**").hasAnyRole("ADMIN", "CLIENT");

                    // Client routes
                    auth.requestMatchers("/skc/Client/**").hasAnyRole("ADMIN", "STAFF", "CLIENT");

                    // Everything else requires authentication
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtfilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordencoder() {
        return new BCryptPasswordEncoder();
    }
}