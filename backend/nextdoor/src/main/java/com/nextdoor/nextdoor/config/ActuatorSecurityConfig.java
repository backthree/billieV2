//package com.nextdoor.nextdoor.config;
//
//import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//public class ActuatorSecurityConfig {
//
//    @Bean
//    @Order(0)
//    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
//        http
//                .securityMatcher(EndpointRequest.toAnyEndpoint())
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()
//                )
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .oauth2Login(auth -> auth.disable())
//                .httpBasic(basic -> basic.disable())
//        ;
//        return http.build();
//    }
//}