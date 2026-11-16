
package com.hotelbooking.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotelbooking.model.User;
import com.hotelbooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthTokenService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthTokenService.class);

    private static final Base64.Encoder ENCODER =
            Base64.getUrlEncoder().withoutPadding();

    private static final Base64.Decoder DECODER =
            Base64.getUrlDecoder();

    private static final String HEADER =
            ENCODER.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
                    .getBytes(StandardCharsets.UTF_8));

    private static final long LIFETIME_SECONDS = 3600;

    private final byte[] signingKey;
    private final ObjectMapper mapper;
    private final UserRepository users;

    public AuthTokenService(
            @Value("${APP_AUTH_SECRET:}") String configuredSecret,
            ObjectMapper mapper,
            UserRepository users) {

        this.mapper = mapper;
        this.users = users;

        if (configuredSecret == null || configuredSecret.isBlank()) {
            signingKey = new byte[32];
            new SecureRandom().nextBytes(signingKey);

            log.warn(
                "APP_AUTH_SECRET is not set: "
                + "a random local-development signing key was generated. "
                + "All tokens will be invalid after restarting the backend."
            );
        } else {
            signingKey =
                configuredSecret.getBytes(StandardCharsets.UTF_8);

            if (signingKey.length < 32) {
                throw new IllegalArgumentException(
                    "APP_AUTH_SECRET must be at least 32 UTF-8 bytes"
                );
            }
        }
    }

    public String issue(User user) {

        if (user == null
                || user.getUserUuid() == null
                || user.getUserUuid().isBlank()) {
            throw new IllegalArgumentException(
                "Valid user UUID is required"
            );
        }

        long now = Instant.now().getEpochSecond();

        ObjectNode claims = mapper.createObjectNode()
                .put("sub", user.getUserUuid())
                .put("iss", "hotel-booking")
                .put("iat", now)
                .put("exp", now + LIFETIME_SECONDS);

        try {
            String payload = ENCODER.encodeToString(
                mapper.writeValueAsBytes(claims)
            );

            String signed = HEADER + "." + payload;

            return signed + "."
                    + ENCODER.encodeToString(sign(signed));

        } catch (Exception ex) {
            throw new IllegalStateException(
                "Cannot issue authentication token", ex
            );
        }
    }

    public Optional<User> authenticate(
            String authorizationHeader) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.length() > 4096) {
            return Optional.empty();
        }

        String[] segments = token.split("\\.", -1);

        if (segments.length != 3
                || !HEADER.equals(segments[0])) {
            return Optional.empty();
        }

        try {
            String signed =
                segments[0] + "." + segments[1];

            if (!MessageDigest.isEqual(
                    sign(signed),
                    DECODER.decode(segments[2]))) {
                return Optional.empty();
            }

            JsonNode claims =
                mapper.readTree(DECODER.decode(segments[1]));

            if (!"hotel-booking".equals(
                    claims.path("iss").asText())) {
                return Optional.empty();
            }

            String uuid = claims.path("sub").asText("");

            long now = Instant.now().getEpochSecond();
            long issuedAt = claims.path("iat").asLong(-1);
            long expiration = claims.path("exp").asLong(-1);

            if (!uuid.matches("[A-Za-z0-9_-]{5,64}")
                    || issuedAt < 0
                    || issuedAt > now + 60
                    || expiration <= now
                    || expiration > issuedAt + LIFETIME_SECONDS) {
                return Optional.empty();
            }


            // FR-82: Chan token cua tai khoan da bi khoa
            return users.findByUserUuid(uuid)
                    .filter(User::isActive);

        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private byte[] sign(String signed) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(
                signingKey, "HmacSHA256"
        ));

        return mac.doFinal(
            signed.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
