package de.pocketcloud.cloud.http.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class AuthTokenGenerator {

    public static String generateAuthToken(int length) {
        byte[] buf = new byte[length];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}