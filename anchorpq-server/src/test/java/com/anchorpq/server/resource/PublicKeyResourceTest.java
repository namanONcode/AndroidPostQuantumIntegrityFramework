package com.anchorpq.server.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for public key endpoint. */
@QuarkusTest
class PublicKeyResourceTest {

    @Test
    @DisplayName("Should return public key in JSON format")
    void testGetPublicKey() {
        given().when()
                .get("/public-key")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicKey", notNullValue())
                .body("publicKey", not(emptyString()))
                .body("parameterSet", notNullValue())
                .body("algorithm", equalTo("ML-KEM"))
                .body("generatedAt", notNullValue())
                .body("keyId", notNullValue());
    }

    @Test
    @DisplayName("Should return raw public key bytes")
    void testGetPublicKeyRaw() {
        given().when()
                .get("/public-key/raw")
                .then()
                .statusCode(200)
                .contentType("application/octet-stream")
                .header("X-Key-Id", notNullValue())
                .header("X-Parameter-Set", notNullValue());
    }

    @Test
    @DisplayName("Should return public key info without key data")
    void testGetPublicKeyInfo() {
        given().when()
                .get("/public-key/info")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicKey", nullValue()) // Key should not be included
                .body("parameterSet", notNullValue())
                .body("algorithm", equalTo("ML-KEM"))
                .body("keyId", notNullValue());
    }

    @Test
    @DisplayName("Should include cache headers")
    void testCacheHeaders() {
        given().when()
                .get("/public-key")
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("max-age"));
    }
}
