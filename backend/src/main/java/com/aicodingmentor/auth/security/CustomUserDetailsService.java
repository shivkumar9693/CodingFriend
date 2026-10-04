package com.aicodingmentor.auth.security;

import com.aicodingmentor.auth.dto.CachedUserAuth;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "user-auth-email", key = "#email.toLowerCase().trim()", unless = "#result == null")
    @Transactional(readOnly = true)
    public CachedUserAuth getCachedAuthByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return CachedUserAuth.from(user);
    }

    @Cacheable(value = "user-auth-id", key = "#id.toString()", unless = "#result == null")
    @Transactional(readOnly = true)
    public CachedUserAuth getCachedAuthById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return CachedUserAuth.from(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return getCachedAuthByEmail(email).toPrincipal();
    }

    public UserDetails loadUserById(UUID id) {
        return getCachedAuthById(id).toPrincipal();
    }

    @Caching(evict = {
            @CacheEvict(value = "user-auth-email", key = "#email.toLowerCase().trim()"),
            @CacheEvict(value = "user-auth-id", allEntries = true)
    })
    public void evictUserAuthCache(String email) {
        // Cache eviction helper when credentials change
    }
}

