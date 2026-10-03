package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.domain.RefreshToken;
import com.aicodingmentor.auth.dto.AuthResponse;
import com.aicodingmentor.auth.dto.GoogleLoginRequest;
import com.aicodingmentor.auth.dto.GoogleUserInfo;
import com.aicodingmentor.auth.dto.LoginRequest;
import com.aicodingmentor.auth.dto.LogoutRequest;
import com.aicodingmentor.auth.dto.RefreshTokenRequest;
import com.aicodingmentor.auth.dto.RegisterRequest;
import com.aicodingmentor.auth.dto.TokenRefreshResponse;
import com.aicodingmentor.auth.dto.UserDto;
import com.aicodingmentor.auth.exception.DuplicateEmailException;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.user.domain.AuthProvider;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            GoogleTokenVerifierService googleTokenVerifierService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw DuplicateEmailException.forEmail(normalizedEmail);
        }

        UserRole role = request.role() != null ? request.role() : UserRole.USER;
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(normalizedEmail, encodedPassword, request.fullName().trim(), role);
        User savedUser = userRepository.save(user);

        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtTokenProvider.getAccessTokenExpirationMs(),
                UserDto.fromUser(savedUser)
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new InvalidTokenException("User account not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtTokenProvider.getAccessTokenExpirationMs(),
                UserDto.fromUser(user)
        );
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verifyToken(request.idToken());
        String normalizedEmail = googleUserInfo.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .map(existingUser -> {
                    boolean updated = false;
                    if (existingUser.getAvatarUrl() == null && googleUserInfo.picture() != null) {
                        existingUser.setAvatarUrl(googleUserInfo.picture());
                        updated = true;
                    }
                    if (existingUser.getProviderId() == null && googleUserInfo.sub() != null) {
                        existingUser.setProviderId(googleUserInfo.sub());
                        updated = true;
                    }
                    return updated ? userRepository.save(existingUser) : existingUser;
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(normalizedEmail);
                    newUser.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                    newUser.setFullName(googleUserInfo.name() != null && !googleUserInfo.name().isBlank()
                            ? googleUserInfo.name().trim()
                            : normalizedEmail.split("@")[0]);
                    newUser.setRole(UserRole.LEARNER);
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setProviderId(googleUserInfo.sub());
                    newUser.setAvatarUrl(googleUserInfo.picture());
                    return userRepository.save(newUser);
                });

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtTokenProvider.getAccessTokenExpirationMs(),
                UserDto.fromUser(user)
        );
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token was not found"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = refreshToken.getUser();
        UserPrincipal userPrincipal = UserPrincipal.create(user);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);

        return TokenRefreshResponse.of(
                newAccessToken,
                refreshToken.getToken(),
                jwtTokenProvider.getAccessTokenExpirationMs()
        );
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenService.deleteByToken(request.refreshToken());
        }
        SecurityContextHolder.clearContext();
    }
}
