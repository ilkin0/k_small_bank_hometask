package com.ilkinmehdiyev.kapitalsmallbankingrest.security;

import static com.ilkinmehdiyev.kapitalsmallbankingrest.common.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.common.HttpHeaders.AUTHORIZATION;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.common.HttpHeaders.CONTENT_TYPE;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.common.HttpHeaders.REQUESTOR_TYPE;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.common.HttpHeaders.X_IDEMPOTENCY_KEY;
import static org.springframework.security.config.Customizer.withDefaults;

import com.ilkinmehdiyev.kapitalsmallbankingrest.service.CustomerServiceImpl;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomerServiceImpl customerService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final String[] permittedUrls = {"/api/v1/auth/**"};

  private final List<String> allowedOrigins = List.of("http://localhost:3000");

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(permittedUrls).permitAll().anyRequest().authenticated())
        .logout(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .addFilterBefore(
            jwtAuthenticationRequestFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return customerService;
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
    authenticationProvider.setUserDetailsService(userDetailsService());
    authenticationProvider.setPasswordEncoder(passwordEncoder);
    return authenticationProvider;
  }

  @Bean
  public JwtAuthenticationRequestFilter jwtAuthenticationRequestFilter() {
    return new JwtAuthenticationRequestFilter(jwtService, userDetailsService());
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        Arrays.asList(
            AUTHORIZATION,
            REQUESTOR_TYPE,
            CONTENT_TYPE,
            ACCESS_CONTROL_ALLOW_HEADERS,
            X_IDEMPOTENCY_KEY));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
