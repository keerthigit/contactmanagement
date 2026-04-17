package com.contactmanagement.contactservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(SecurityUsersProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/contacts/**").hasAnyRole("CONTACT_READ", "CONTACT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/contacts/**").hasAnyRole("CONTACT_WRITE", "CONTACT_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/contacts/**").hasAnyRole("CONTACT_WRITE", "CONTACT_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/contacts/**").hasAnyRole("CONTACT_WRITE", "CONTACT_ADMIN")
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(SecurityUsersProperties properties, PasswordEncoder passwordEncoder) {
        UserDetails readUser = User.withUsername(properties.getRead().getUsername())
                .password(passwordEncoder.encode(properties.getRead().getPassword()))
                .roles("CONTACT_READ")
                .build();

        UserDetails writeUser = User.withUsername(properties.getWrite().getUsername())
                .password(passwordEncoder.encode(properties.getWrite().getPassword()))
                .roles("CONTACT_WRITE")
                .build();

        UserDetails adminUser = User.withUsername(properties.getAdmin().getUsername())
                .password(passwordEncoder.encode(properties.getAdmin().getPassword()))
                .roles("CONTACT_ADMIN")
                .build();

        return new InMemoryUserDetailsManager(readUser, writeUser, adminUser);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
