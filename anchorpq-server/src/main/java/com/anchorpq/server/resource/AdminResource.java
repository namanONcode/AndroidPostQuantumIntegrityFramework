package com.anchorpq.server.resource;

import com.anchorpq.server.model.IntegrityRecord;
import com.anchorpq.server.repository.IntegrityRepository;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource for managing canonical integrity records.
 *
 * <p>This resource is typically used by:
 *
 * <ul>
 *   <li>CI/CD pipelines to register new app versions
 *   <li>Administrators to manage integrity records
 * </ul>
 *
 * <p><b>Security Note:</b> In production, these endpoints should be protected with authentication
 * (e.g., API key, OAuth2).
 */
@Path("/admin/records")
@Tag(name = "Administration", description = "Integrity record management endpoints")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject IntegrityRepository integrityRepository;

    /**
     * Creates or updates a canonical integrity record.
     *
     * @param record The integrity record to create/update
     * @return The created or updated record
     */
    @POST
    @Transactional
    @Operation(
            summary = "Create or update integrity record",
            description =
                    "Registers a canonical integrity record for a specific application version and variant")
    @APIResponses({
        @APIResponse(
                responseCode = "201",
                description = "Record created successfully",
                content = @Content(schema = @Schema(implementation = IntegrityRecord.class))),
        @APIResponse(
                responseCode = "200",
                description = "Record updated successfully",
                content = @Content(schema = @Schema(implementation = IntegrityRecord.class))),
        @APIResponse(responseCode = "400", description = "Invalid record data")
    })
    public Response createOrUpdateRecord(@Valid IntegrityRecord record) {
        try {
            boolean isUpdate =
                    integrityRepository.existsByVersionAndVariant(
                            record.getVersion(), record.getVariant());

            IntegrityRecord savedRecord = integrityRepository.saveOrUpdate(record);

            Log.info(
                    (isUpdate ? "Updated" : "Created")
                            + " integrity record for version: "
                            + record.getVersion()
                            + ", variant: "
                            + record.getVariant());

            return Response.status(isUpdate ? Response.Status.OK : Response.Status.CREATED)
                    .entity(savedRecord)
                    .build();

        } catch (Exception e) {
            Log.error("Failed to save integrity record", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Retrieves an integrity record by version and variant.
     *
     * @param version Application version
     * @param variant Build variant
     * @return The integrity record if found
     */
    @GET
    @Path("/{version}/{variant}")
    @Operation(
            summary = "Get integrity record",
            description = "Retrieves a canonical integrity record by version and variant")
    @APIResponses({
        @APIResponse(
                responseCode = "200",
                description = "Record found",
                content = @Content(schema = @Schema(implementation = IntegrityRecord.class))),
        @APIResponse(responseCode = "404", description = "Record not found")
    })
    public Response getRecord(
            @Parameter(description = "Application version", required = true) @PathParam("version")
                    String version,
            @Parameter(description = "Build variant", required = true) @PathParam("variant")
                    String variant) {

        Optional<IntegrityRecord> record =
                integrityRepository.findByVersionAndVariant(version, variant);

        if (record.isPresent()) {
            return Response.ok(record.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Record not found\"}")
                    .build();
        }
    }

    /**
     * Lists all integrity records with optional filtering.
     *
     * @param version Optional version filter
     * @param variant Optional variant filter
     * @return List of matching records
     */
    @GET
    @Operation(
            summary = "List integrity records",
            description = "Lists all canonical integrity records with optional filtering")
    @APIResponse(
            responseCode = "200",
            description = "Records retrieved successfully",
            content = @Content(schema = @Schema(implementation = IntegrityRecord.class)))
    public Response listRecords(
            @Parameter(description = "Filter by version") @QueryParam("version") String version,
            @Parameter(description = "Filter by variant") @QueryParam("variant") String variant) {

        List<IntegrityRecord> records;

        if (version != null && !version.isEmpty()) {
            records = integrityRepository.findByVersion(version);
        } else if (variant != null && !variant.isEmpty()) {
            records = integrityRepository.findByVariant(variant);
        } else {
            records = integrityRepository.findAllActive();
        }

        return Response.ok(records).build();
    }

    /**
     * Deletes (deactivates) an integrity record.
     *
     * @param version Application version
     * @param variant Build variant
     * @return Success or not found response
     */
    @DELETE
    @Path("/{version}/{variant}")
    @Transactional
    @Operation(
            summary = "Delete integrity record",
            description = "Deactivates an integrity record (soft delete)")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Record deleted"),
        @APIResponse(responseCode = "404", description = "Record not found")
    })
    public Response deleteRecord(
            @Parameter(description = "Application version", required = true) @PathParam("version")
                    String version,
            @Parameter(description = "Build variant", required = true) @PathParam("variant")
                    String variant) {

        Optional<IntegrityRecord> record =
                integrityRepository.findByVersionAndVariant(version, variant);

        if (record.isPresent()) {
            IntegrityRecord existingRecord = record.get();
            existingRecord.setActive(false);
            integrityRepository.persist(existingRecord);

            Log.info(
                    "Deactivated integrity record for version: "
                            + version
                            + ", variant: "
                            + variant);
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Record not found\"}")
                    .build();
        }
    }

    /**
     * Bulk import integrity records.
     *
     * @param records List of records to import
     * @return Import results
     */
    @POST
    @Path("/bulk")
    @Transactional
    @Operation(
            summary = "Bulk import records",
            description = "Imports multiple integrity records at once")
    public Response bulkImport(List<@Valid IntegrityRecord> records) {
        try {
            int created = 0;
            int updated = 0;

            for (IntegrityRecord record : records) {
                boolean isUpdate =
                        integrityRepository.existsByVersionAndVariant(
                                record.getVersion(), record.getVariant());
                integrityRepository.saveOrUpdate(record);

                if (isUpdate) updated++;
                else created++;
            }

            Log.info("Bulk import completed: " + created + " created, " + updated + " updated");

            return Response.ok("{\"created\": " + created + ", \"updated\": " + updated + "}")
                    .build();

        } catch (Exception e) {
            Log.error("Bulk import failed", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
