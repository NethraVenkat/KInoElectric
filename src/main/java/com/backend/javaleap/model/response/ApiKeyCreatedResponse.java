package com.backend.javaleap.model.response;

/** Returned once when a key is created - the plain key cannot be retrieved again. */
public record ApiKeyCreatedResponse(ApiKeyResponse key, String apiKey, String warning) {
}
