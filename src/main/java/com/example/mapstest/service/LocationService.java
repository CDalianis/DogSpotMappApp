package com.example.mapstest.service;

import com.example.mapstest.core.exceptions.EntityAlreadyExistsException;
import com.example.mapstest.core.exceptions.EntityInvalidArgumentException;
import com.example.mapstest.core.exceptions.EntityNotFoundException;
import com.example.mapstest.core.exceptions.FileUploadException;
import com.example.mapstest.core.exceptions.ValidationException;
import com.example.mapstest.model.Location;
import com.example.mapstest.repository.LocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
public class LocationService {

    private static final int MAX_PHOTO_PAYLOAD_CHARS = 512 * 1024;

    private final LocationRepository locationRepository;
    private final Validator validator;

    public LocationService(LocationRepository locationRepository, Validator validator) {
        this.locationRepository = locationRepository;
        this.validator = validator;
    }

    public List<Location> getLocations() {
        List<Location> locations = locationRepository.findAllWithPhotosSorted();
        log.debug("Fetched {} locations", locations.size());
        return locations;
    }

    @Transactional
    public Location addLocation(Location location) {
        applyDefaults(location);
        validateLocation(location);
        assertPhotosWithinLimit(location.getPhotos());
        if (location.getId() != null && locationRepository.existsById(location.getId())) {
            throw new EntityAlreadyExistsException("LOCATION", "A location with id " + location.getId() + " already exists");
        }
        location.setId(null);
        Location saved = locationRepository.save(location);
        log.info("Created location id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public Location updateLocation(Long id, Location updated) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("LOCATION", "Location not found: " + id));
        applyDefaults(updated);
        assertPathAndBodyIdMatch(id, updated);
        validateLocation(updated);
        assertPhotosWithinLimit(updated.getPhotos());
        existing.setName(updated.getName());
        existing.setNotes(updated.getNotes());
        existing.setLat(updated.getLat());
        existing.setLng(updated.getLng());
        existing.setCategory(updated.getCategory());
        existing.setVisited(updated.getVisited());
        existing.setFavorite(updated.getFavorite());
        existing.setRating(updated.getRating());
        existing.setPhotos(new ArrayList<>(updated.getPhotos() == null ? List.of() : updated.getPhotos()));
        Location saved = locationRepository.save(existing);
        log.info("Updated location id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public void deleteLocation(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new EntityNotFoundException("LOCATION", "Location not found: " + id);
        }
        locationRepository.deleteById(id);
        log.info("Deleted location id={}", id);
    }

    @Transactional
    public void replaceAll(List<Location> imported) {
        if (imported == null) {
            locationRepository.deleteAll();
            log.info("Import cleared all locations");
            return;
        }
        List<Location> validated = new ArrayList<>(imported.size());
        for (Location loc : imported) {
            Location copy = copyForImport(loc);
            applyDefaults(copy);
            validateLocation(copy);
            assertPhotosWithinLimit(copy.getPhotos());
            validated.add(copy);
        }
        locationRepository.deleteAll();
        for (Location loc : validated) {
            loc.setId(null);
            locationRepository.save(loc);
        }
        log.info("Import replaced all locations with {} entries", validated.size());
    }

    private static Location copyForImport(Location loc) {
        return new Location(
                null,
                loc.getLat(),
                loc.getLng(),
                loc.getName(),
                loc.getNotes(),
                loc.getCategory(),
                loc.getVisited(),
                loc.getFavorite(),
                loc.getRating(),
                loc.getPhotos() == null ? new ArrayList<>() : new ArrayList<>(loc.getPhotos()));
    }

    private static void applyDefaults(Location location) {
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
            location.setPhotos(new ArrayList<>());
        }
    }

    private void validateLocation(Location location) {
        BindingResult errors = new BeanPropertyBindingResult(location, "location");
        validator.validate(location, errors);
        if (errors.hasErrors()) {
            throw new ValidationException("LOCATION", "Location validation failed", errors);
        }
    }

    private static void assertPathAndBodyIdMatch(Long pathId, Location updated) {
        if (updated.getId() != null && !updated.getId().equals(pathId)) {
            throw new EntityInvalidArgumentException("LOCATION", "Path id and body id must match");
        }
    }

    private static void assertPhotosWithinLimit(List<String> photos) {
        if (photos == null) {
            return;
        }
        for (String photo : photos) {
            if (photo != null && photo.length() > MAX_PHOTO_PAYLOAD_CHARS) {
                throw new FileUploadException("LOCATION", "Photo payload exceeds maximum allowed size");
            }
        }
    }
}
