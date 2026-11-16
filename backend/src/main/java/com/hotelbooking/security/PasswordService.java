
package com.hotelbooking.security;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {

    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;

    private static final Base64.Encoder ENC =
            Base64.getUrlEncoder().withoutPadding();

    private static final Base64.Decoder DEC =
            Base64.getUrlDecoder();

    public String hash(String password) {

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        return "pbkdf2_sha256$"
                + ITERATIONS + "$"
                + ENC.encodeToString(salt) + "$"
                + ENC.encodeToString(
                    derive(password, salt, ITERATIONS)
                );
    }

    public boolean matches(String password, String stored) {

        if (password == null
                || password.isEmpty()
                || stored == null) {
            return false;
        }

        try {
            if (stored.startsWith("pbkdf2_sha256$")) {

                String[] parts = stored.split("\\$", -1);

                if (parts.length != 4) {
                    return false;
                }

                int n = Integer.parseInt(parts[1]);

                if (n < 100_000 || n > 1_000_000) {
                    return false;
                }

                byte[] salt = DEC.decode(parts[2]);
                byte[] expected = DEC.decode(parts[3]);

                return salt.length == 16
                        && expected.length == 32
                        && MessageDigest.isEqual(
                            expected,
                            derive(password, salt, n)
                        );
            }

            // Ho tro du lieu mau SHA-256 cu.
            if (!stored.matches("(?i)[0-9a-f]{64}")) {
                return false;
            }

            byte[] actual =
                MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(
                        StandardCharsets.UTF_8
                    ));

            return MessageDigest.isEqual(
                actual,
                java.util.HexFormat.of().parseHex(stored)
            );

        } catch (Exception ex) {
            return false;
        }
    }

    public boolean needsUpgrade(String stored) {
        return stored != null
                && !stored.startsWith("pbkdf2_sha256$");
    }

    private byte[] derive(
            String password,
            byte[] salt,
            int iterations) {

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            KEY_BITS
        );

        try {
            return SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

        } catch (Exception ex) {
            throw new IllegalStateException(
                "Password hash failed", ex
            );

        } finally {
            spec.clearPassword();
        }
    }
}
