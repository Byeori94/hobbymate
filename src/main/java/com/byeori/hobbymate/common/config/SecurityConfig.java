package com.byeori.hobbymate.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.byeori.hobbymate.auth.security.SavedIdAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SavedIdAuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/auth/login",
                                "/auth/join",
                                "/auth/check-id",
                                "/auth/check-nickname",
                                "/auth/check-email",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/profile-images/**",
                                "/club-images/**",
                                "/favicon.ico",
                                "/error")
                        .permitAll()
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/member/mypage", "/member/mypage/**")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/clubs/new")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/clubs", "/clubs/*")
                        .permitAll()
                        .requestMatchers("/clubs/**")
                        .hasRole("USER")
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/auth/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
