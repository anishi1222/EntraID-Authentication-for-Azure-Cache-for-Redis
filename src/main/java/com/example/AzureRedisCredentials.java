package com.example;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import io.lettuce.core.RedisCredentials;

import java.util.Objects;

public class AzureRedisCredentials implements RedisCredentials {
    private TokenRequestContext tokenRequestContext = new TokenRequestContext()
            .addScopes("acca5fbb-b7e4-4009-81f1-37e38fd66d78/.default");
    private TokenCredential tokenCredential;
    private final String username;

    public AzureRedisCredentials(String username, TokenCredential tokenCredential) {
        Objects.requireNonNull(username, "Username is required");
        Objects.requireNonNull(tokenCredential, "Token Credential is required");
        this.username = username;
        this.tokenCredential = tokenCredential;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean hasUsername() {
        return username != null;
    }

    @Override
    public char[] getPassword() {
        try {
            return Objects.requireNonNull(tokenCredential.getToken(tokenRequestContext).block()).getToken().toCharArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasPassword() {
        return tokenCredential != null;
    }
}
