package com.anchorpq.server.resource;

import com.anchorpq.server.crypto.MLKemService;
import com.anchorpq.server.model.PublicKeyResponse;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource for ML-KEM public key distribution.
 *
 * <p>This resource exposes the server's ML-KEM public key so that clients can:
 *
 * <ol>
 *   <li>Fetch the public key
 *   <li>Use it to encapsulate a shared secret
 *   <li>Encrypt their integrity payload for secure transmission
 * </ol>
 */
@Path("/public-key")
@Tag(name = "Key Exchange", description = "ML-KEM public key distribution")
@Produces(MediaType.APPLICATION_JSON)
public class PublicKeyResource {

    @Inject MLKemService mlKemService;

    /**
     * Returns the server's ML-KEM public key for client key encapsulation.
     *
     * <p>Clients should:
     *
     * <ol>
     *   <li>Call this endpoint to get the server's public key
     *   <li>Parse the Base64-encoded public key
     *   <li>Use ML-KEM encapsulation to generate a shared secret
     *   <li>Derive an AES-256 key from the shared secret
     *   <li>Encrypt integrity payload with AES-GCM
     * </ol>
     *
     * @return PublicKeyResponse containing the ML-KEM public key
     */
    @GET
    @Operation(
            summary = "Get ML-KEM public key",
            description =
                    "Returns the server's ML-KEM (CRYSTALS-Kyber) public key for post-quantum key encapsulation")
    @APIResponses({
        @APIResponse(
                responseCode = "200",
                description = "Public key retrieved successfully",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = PublicKeyResponse.class))),
        @APIResponse(responseCode = "500", description = "Server key not available")
    })
    public Response getPublicKey() {
        try {
            String publicKeyBase64 = mlKemService.getPublicKeyBase64();
            String parameterSet = mlKemService.getParameterSet();
            String keyId = mlKemService.getKeyId();
            long generatedAt = mlKemService.getKeyGeneratedAt();

            PublicKeyResponse response =
                    new PublicKeyResponse(
                            publicKeyBase64, parameterSet, "ML-KEM", generatedAt, keyId);

            Log.debug("Public key requested, key ID: " + keyId);

            return Response.ok(response)
                    .header("Cache-Control", "public, max-age=3600") // Cache for 1 hour
                    .build();

        } catch (Exception e) {
            Log.error("Failed to retrieve public key", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Public key not available\"}")
                    .build();
        }
    }

    /**
     * Returns the raw public key bytes for clients that prefer binary format.
     *
     * @return Raw public key bytes
     */
    @GET
    @Path("/raw")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(
            summary = "Get raw ML-KEM public key",
            description = "Returns the raw bytes of the server's ML-KEM public key")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Public key bytes retrieved successfully"),
        @APIResponse(responseCode = "500", description = "Server key not available")
    })
    public Response getPublicKeyRaw() {
        try {
            byte[] publicKeyBytes = mlKemService.getPublicKeyBytes();

            return Response.ok(publicKeyBytes)
                    .header("Content-Disposition", "attachment; filename=\"mlkem-public-key.bin\"")
                    .header("X-Key-Id", mlKemService.getKeyId())
                    .header("X-Parameter-Set", mlKemService.getParameterSet())
                    .header("Cache-Control", "public, max-age=3600")
                    .build();

        } catch (Exception e) {
            Log.error("Failed to retrieve raw public key", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Returns metadata about the current key without the key itself.
     *
     * @return Key metadata (parameter set, key ID, generation time)
     */
    @GET
    @Path("/info")
    @Operation(
            summary = "Get public key metadata",
            description =
                    "Returns information about the server's ML-KEM public key without the key itself")
    public Response getPublicKeyInfo() {
        try {
            PublicKeyResponse response =
                    new PublicKeyResponse(
                            null, // Don't include the key
                            mlKemService.getParameterSet(),
                            "ML-KEM",
                            mlKemService.getKeyGeneratedAt(),
                            mlKemService.getKeyId());

            return Response.ok(response).build();

        } catch (Exception e) {
            Log.error("Failed to retrieve public key info", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
