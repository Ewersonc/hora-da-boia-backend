package br.com.horadaboia.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Segurança da API: sem estado, autenticação por token JWT, senhas com BCrypt e autorização
 * por perfil nos controllers (@PreAuthorize) (RF02, RF03, RNF03, RNF04).
 */
@Configuration
@EnableMethodSecurity
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, JwtService jwtService,
                                       CorsConfigurationSource fonteCors) throws Exception {
        http
                .cors(cors -> cors.configurationSource(fonteCors))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(erros -> erros.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/favicon.ico", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/disponibilidade").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS: o frontend roda em outro endereço (por exemplo, Live Server em localhost:5500) e precisa
     * ser autorizado a chamar a API. As origens vêm de app.cors.origens (variável CORS_ORIGENS).
     */
    @Bean
    public CorsConfigurationSource fonteCors(@Value("${app.cors.origens}") String[] origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origens));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
