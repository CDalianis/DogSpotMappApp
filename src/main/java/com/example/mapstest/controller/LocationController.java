package com.example.mapstest.controller;

import com.example.mapstest.dto.ErrorResponseDTO;
import com.example.mapstest.dto.ValidationErrorResponseDTO;
import com.example.mapstest.model.Location;
import com.example.mapstest.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "CRUD and bulk import/export for dog spot locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "List all locations", description = "Returns every location with photos, sorted by id.")
    @ApiResponse(responseCode = "200", description = "Locations found",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Location.class))))
    public List<Location> getLocations() {
        return locationService.getLocations();
    }

    @PostMapping
    @Operation(summary = "Create a location", description = "Adds a new location. Server assigns id; omit id or send null.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location created",
                    content = @Content(schema = @Schema(implementation = Location.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid argument",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Location with given id already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Photo payload too large or server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Location addLocation(@RequestBody Location location) {
        return locationService.addLocation(location);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a location", description = "Updates an existing location by path id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated",
                    content = @Content(schema = @Schema(implementation = Location.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed, invalid argument, or path/body id mismatch",
                    content = @Content(schema = @Schema(oneOf = {ValidationErrorResponseDTO.class, ErrorResponseDTO.class}))),
            @ApiResponse(responseCode = "404", description = "Location not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Photo payload too large or server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Location updateLocation(
            @Parameter(description = "Location id") @PathVariable Long id,
            @RequestBody Location location) {
        return locationService.updateLocation(id, location);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a location", description = "Removes a location by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location deleted"),
            @ApiResponse(responseCode = "404", description = "Location not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public void deleteLocation(@Parameter(description = "Location id") @PathVariable Long id) {
        locationService.deleteLocation(id);
    }

    @PostMapping("/import")
    @Operation(summary = "Replace all locations", description = "Deletes existing locations and imports the provided list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import completed"),
            @ApiResponse(responseCode = "400", description = "Validation failed on one or more locations",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Photo payload too large or server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public void importLocations(@RequestBody List<Location> locations) {
        locationService.replaceAll(locations);
    }

    @GetMapping("/export")
    @Operation(summary = "Export all locations", description = "Returns all locations (same payload as list).")
    @ApiResponse(responseCode = "200", description = "Locations exported",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Location.class))))
    public List<Location> exportLocations() {
        return locationService.getLocations();
    }
}
