package com.example.mapstest.repository;

import com.example.mapstest.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT DISTINCT l FROM Location l LEFT JOIN FETCH l.photos ORDER BY l.id")
    List<Location> findAllWithPhotosSorted();
}
