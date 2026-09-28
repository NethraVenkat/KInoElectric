package com.backend.javaleap.service;

import com.backend.javaleap.entity.ApiClient;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.Role;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.UserPrincipal;
import com.backend.javaleap.model.request.ApiKeyCreateRequest;
import com.backend.javaleap.model.response.ApiKeyCreatedResponse;
import com.backend.javaleap.model.response.ApiKeyResponse;
import com.backend.javaleap.repository.ApiClientRepository;
import com.backend.javaleap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * API keys for the System actor. Keys look like {@code pdk_sys_XXXXXXXX...} and are
 * stored only as SHA-256 hashes. A valid key authenticates as the linked SYSTEM user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    public static final String HEADER = "X-API-KEY";
    public static final String PREFIX = "pdk_sys_";
    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiClientRepository apiClientRepository;
    private final UserRepository userRepository;
    private final KineticMapper mapper;

    /** Validates a raw key; returns the principal it acts as. */
    @Transactional
    public Optional<UserPrincipal> authenticate(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(PREFIX) || rawKey.length() > 128) {
            return Optional.empty();
        }
        return apiClientRepository.findByKeyHash(sha256(rawKey))
                .filter(ApiClient::isUsable)
                .filter(c -> Boolean.TRUE.equals(c.getUser().getEnabled()))
                .map(c -> {
                    LocalDateTime now = LocalDateTime.now();
                    if (c.getLastUsedAt() == null || c.getLastUsedAt().isBefore(now.minusMinutes(1))) {
                        c.setLastUsedAt(now);
                    }
                    return new UserPrincipal(c.getUser());
                });
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list() {
        return apiClientRepository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public ApiKeyCreatedResponse create(ApiKeyCreateRequest req) {
        User systemUser = userRepository.findByRoleOrderByUsernameAsc(Role.SYSTEM).stream()
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No enabled SYSTEM user exists to link the key to"));

        String rawKey = PREFIX + randomToken(32);
        ApiClient client = apiClientRepository.save(ApiClient.builder()
                .name(req.name().trim())
                .keyPrefix(PREFIX)
                .keyHash(sha256(rawKey))
                .user(systemUser)
                .enabled(true)
                .expiresAt(req.expiresAt())
                .build());
        log.info("API key '{}' created (acts as '{}')", client.getName(), systemUser.getUsername());
        return new ApiKeyCreatedResponse(mapper.toResponse(client), rawKey,
                "Copy this key now - it is stored hashed and cannot be shown again.");
    }

    @Transactional
    public ApiKeyResponse revoke(Long id) {
        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key", id));
        client.setEnabled(false);
        log.info("API key '{}' revoked", client.getName());
        return mapper.toResponse(client);
    }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String randomToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
