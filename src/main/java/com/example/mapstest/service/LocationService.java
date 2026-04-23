package com.example.mapstest.service;

import com.example.mapstest.model.Location;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LocationService {

    private final List<Location> locations = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(1L);

    public LocationService() {
        addLocation(new Location(null, 37.9838, 23.7275, "Athens Center", null, "PARK", false, true, 5, List.of()));
        addLocation(new Location(null, 37.9715, 23.7257, "Acropolis", "Busy area, keep dog close", "OTHER", true, false, 4, List.of()));
        addLocation(new Location(null, 37.9680, 23.7286, "Plaka", "Nice evening walk", "CAFE", false, false, 4, List.of()));
    }

    public List<Location> getLocations() {
        return locations;
    }

    public Location addLocation(Location location) {
        if (location.getId() == null) {
            location.setId(idSequence.getAndIncrement());
        }
        if (location.getCategory() == null || location.getCategory().isBlank()) {
            location.setCategory("OTHER");
        }
        if (location.getVisited() == null) {
            location.setVisited(false);
        }
        if (location.getFavorite() == null) {
            location.setFavorite(false);
        }
        if (location.getPhotos() == null) {
            location.setPhotos(List.of());
        }
        locations.add(location);
        return location;
    }

    public Optional<Location> updateLocation(Long id, Location updated) {
        return locations.stream()
                .filter(loc -> loc.getId().equals(id))
                .findFirst()
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setNotes(updated.getNotes());
                    existing.setLat(updated.getLat());
                    existing.setLng(updated.getLng());
                    existing.setCategory(updated.getCategory());
                    existing.setVisited(updated.getVisited());
                    existing.setFavorite(updated.getFavorite());
                    existing.setRating(updated.getRating());
                    existing.setPhotos(updated.getPhotos() == null ? List.of() : updated.getPhotos());
                    return existing;
                });
    }

    public boolean deleteLocation(Long id) {
        return locations.removeIf(loc -> loc.getId().equals(id));
    }

    public void replaceAll(List<Location> imported) {
        locations.clear();
        idSequence.set(1L);
        if (imported == null) {
            return;
        }
        for (Location loc : imported) {
            loc.setId(null);
            addLocation(loc);
        }
    }
}