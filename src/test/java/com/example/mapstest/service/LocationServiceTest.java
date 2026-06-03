package com.example.mapstest.service;

import com.example.mapstest.core.exceptions.EntityAlreadyExistsException;
import com.example.mapstest.core.exceptions.EntityNotFoundException;
import com.example.mapstest.core.exceptions.FileUploadException;
import com.example.mapstest.model.Location;
import com.example.mapstest.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        locationService = new LocationService(locationRepository, validator);
    }

    @Test
    void addLocation_appliesDefaultsAndSaves() {
        Location input = new Location(null, 37.98, 23.72, "Park", null, null, null, null, null, null);
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Location created = locationService.addLocation(input);

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getCategory()).isEqualTo("OTHER");
        assertThat(created.getVisited()).isFalse();
        assertThat(created.getFavorite()).isFalse();
        assertThat(created.getPhotos()).isEmpty();
    }

    @Test
    void addLocation_throwsWhenIdAlreadyExists() {
        Location input = new Location(5L, 37.98, 23.72, "Park", null, "PARK", false, false, null, new ArrayList<>());
        when(locationRepository.existsById(5L)).thenReturn(true);

        assertThatThrownBy(() -> locationService.addLocation(input))
                .isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    void updateLocation_updatesExistingFields() {
        Location existing = new Location(1L, 37.0, 23.0, "Old", "notes", "PARK", false, false, 3, new ArrayList<>());
        Location update = new Location(1L, 38.0, 24.0, "New", "updated", "CAFE", true, true, 5, List.of("data:image/png;base64,abc"));

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Location saved = locationService.updateLocation(1L, update);

        assertThat(saved.getName()).isEqualTo("New");
        assertThat(saved.getCategory()).isEqualTo("CAFE");
        assertThat(saved.getFavorite()).isTrue();
        assertThat(saved.getRating()).isEqualTo(5);
    }

    @Test
    void updateLocation_throwsWhenNotFound() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        Location update = new Location(99L, 37.0, 23.0, "X", null, "PARK", false, false, null, new ArrayList<>());

        assertThatThrownBy(() -> locationService.updateLocation(99L, update))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteLocation_throwsWhenNotFound() {
        when(locationRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> locationService.deleteLocation(7L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteLocation_removesExisting() {
        when(locationRepository.existsById(2L)).thenReturn(true);

        locationService.deleteLocation(2L);

        verify(locationRepository).deleteById(2L);
    }

    @Test
    void replaceAll_clearsDatabaseWhenNull() {
        locationService.replaceAll(null);

        verify(locationRepository).deleteAll();
        verify(locationRepository, never()).save(any());
    }

    @Test
    void replaceAll_replacesWithValidatedLocations() {
        Location first = new Location(null, 37.98, 23.72, "A", null, "PARK", false, false, 4, new ArrayList<>());
        Location second = new Location(null, 37.99, 23.73, "B", null, "CAFE", false, false, 3, new ArrayList<>());

        locationService.replaceAll(List.of(first, second));

        verify(locationRepository).deleteAll();
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(loc -> loc.getId() == null);
    }

    @Test
    void addLocation_throwsWhenPhotoTooLarge() {
        String huge = "x".repeat(512 * 1024 + 1);
        Location input = new Location(null, 37.98, 23.72, "Park", null, "PARK", false, false, null, List.of(huge));

        assertThatThrownBy(() -> locationService.addLocation(input))
                .isInstanceOf(FileUploadException.class);
    }
}
