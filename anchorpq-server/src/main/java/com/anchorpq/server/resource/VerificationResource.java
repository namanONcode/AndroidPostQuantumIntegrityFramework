package com.anchorpq.server.resource;

import com.anchorpq.server.crypto.CryptoException;
import com.anchorpq.server.crypto.CryptoService;
import com.anchorpq.server.model.IntegrityPayload;
import com.anchorpq.server.model.VerificationRequest;
import com.anchorpq.server.model.VerificationResponse;
import com.anchorpq.server.service.IntegrityVerificationService;
import com.anchorpq.server.service.RateLimitService;
import io.quarkus.logging.Log;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource for integrity verification endpoints.
 *
 * <p>This resource handles the main verification flow:
 *
 * <ol>
 *   <li>Receives encrypted verification requests
 *   <li>Decrypts using ML-KEM + AES-GCM
 *   <li>Validates integrity against canonical records
 *   <li>Returns verification decision
 * </ol>
 */
@Path("/verify")
@Tag(name = "Verification", description = "Integrity verification endpoints")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VerificationResource {

    @Inject CryptoService cryptoService;

    @Inject IntegrityVerificationService verificationService;

    @Inject RateLimitService rateLimitService;

    /**
     * Verifies application integrity from an encrypted request.
     *
     * <p>The client must:
     *
     * <ol>
     *   <li>Fetch the server's ML-KEM public key from /public-key
     *   <li>Encapsulate a shared secret using the public key
     *   <li>Derive an AES-256 key using HKDF-SHA3
     *   <li>Encrypt the integrity payload using AES-GCM
     *   <li>Send the encapsulated key and encrypted payload to this endpoint
     * </ol>
     *
     * @param request The encrypted verification request
     * @param httpRequest The HTTP request for client IP extraction
     * @return VerificationResponse with status (APPROVED/RESTRICTED/REJECTED)
     */
    @POST
    @Operation(
            summary = "Verify application integrity",
            description =
                    "Receives an ML-KEM encrypted integrity payload and verifies it against canonical records")
    @APIResponses({
        @APIResponse(
                responseCode = "200",
                description = "Verification completed",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = VerificationResponse.class))),
        @APIResponse(
                responseCode = "400",
                description = "Invalid request format",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = VerificationResponse.class))),
        @APIResponse(
                responseCode = "429",
                description = "Rate limit exceeded",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = VerificationResponse.class))),
        @APIResponse(
                responseCode = "500",
                description = "Internal server error",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = VerificationResponse.class)))
    })
    public Response verify(
            @Valid VerificationRequest request, @Context HttpServerRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        // Check rate limit
        if (!rateLimitService.isAllowed(clientIp)) {
            Log.warn("Rate limit exceeded for client: " + clientIp);
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(VerificationResponse.rejected("Rate limit exceeded", "ERR_RATE_LIMIT"))
                    .header("Retry-After", "60")
                    .build();
        }

        try {
            // Decrypt and parse the verification request
            IntegrityPayload payload = cryptoService.decryptVerificationRequest(request);

            // Verify integrity against canonical records
            VerificationResponse response = verificationService.verifyIntegrity(payload);

            Response.Status status =
                    switch (response.getStatus()) {
                        case APPROVED -> Response.Status.OK;
                        case RESTRICTED -> Response.Status.OK;
                        case REJECTED -> Response.Status.OK; // Still 200, status in body
                    };

            return Response.status(status)
                    .entity(response)
                    .header(
                            "X-RateLimit-Remaining",
                            rateLimitService.getRemainingRequests(clientIp))
                    .build();

        } catch (CryptoException e) {
            Log.error("Cryptographic error during verification", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            VerificationResponse.rejected(
                                    "Cryptographic verification failed", e.getErrorCodeString()))
                    .build();

        } catch (IllegalArgumentException e) {
            Log.warn("Invalid verification request: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            VerificationResponse.rejected(
                                    "Invalid request: " + e.getMessage(), "ERR_INVALID_REQUEST"))
                    .build();

        } catch (Exception e) {
            Log.error("Unexpected error during verification", e);
            // Don't expose internal error details in production
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(VerificationResponse.rejected("Internal server error", "ERR_INTERNAL"))
                    .build();
        }
    }

    /** Extracts the client IP from the HTTP request, considering proxies. */
    private String getClientIp(HttpServerRequest request) {
        // Check X-Forwarded-For header for proxied requests
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // Take the first IP (original client)
            return forwarded.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // Fall back to remote address
        return request.remoteAddress() != null ? request.remoteAddress().host() : "unknown";
    }
}
