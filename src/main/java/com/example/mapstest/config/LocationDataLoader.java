package com.example.mapstest.config;

import com.example.mapstest.model.Location;
import com.example.mapstest.repository.LocationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LocationDataLoader implements CommandLineRunner {

    private final LocationRepository locationRepository;

    public LocationDataLoader(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public void run(String... args) {
        if (locationRepository.count() > 0) {
            return;
        }
        locationRepository.saveAll(List.of(
                new Location(null, 37.9838, 23.7275, "Athens Center", null, "PARK", false, true, 5, new ArrayList<>()),
                new Location(null, 37.9715, 23.7257, "Acropolis", "Busy area, keep dog close", "OTHER", true, false, 4, new ArrayList<>()),
                new Location(null, 37.9680, 23.7286, "Plaka", "Nice evening walk", "CAFE", false, false, 4, new ArrayList<>())
        ));
    }
}
