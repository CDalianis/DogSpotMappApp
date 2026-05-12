package com.example.mapstest.controller;

import com.example.mapstest.model.Location;
import com.example.mapstest.service.LocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<Location> getLocations() {
        return locationService.getLocations();
    }

    @PostMapping
    public Location addLocation(@RequestBody Location location) {
        return locationService.addLocation(location);
    }

    @PutMapping("/{id}")
    public Location updateLocation(@PathVariable Long id, @RequestBody Location location) {
        return locationService.updateLocation(id, location);
    }

    @DeleteMapping("/{id}")
    public void deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
    }

    @PostMapping("/import")
    public void importLocations(@RequestBody List<Location> locations) {
        locationService.replaceAll(locations);
    }

    @GetMapping("/export")
    public List<Location> exportLocations() {
        return locationService.getLocations();
    }
}
