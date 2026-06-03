package com.example.mapstest.controller;

import com.example.mapstest.core.ErrorHandler;
import com.example.mapstest.model.Location;
import com.example.mapstest.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@Import(ErrorHandler.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationService locationService;

    @Test
    void getLocations_returnsList() throws Exception {
        when(locationService.getLocations()).thenReturn(List.of(
                new Location(1L, 37.98, 23.72, "Park", null, "PARK", false, true, 4, List.of())
        ));

        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Park"));
    }

    @Test
    void addLocation_returnsCreated() throws Exception {
        Location saved = new Location(1L, 37.98, 23.72, "Park", null, "PARK", false, false, null, List.of());
        when(locationService.addLocation(any(Location.class))).thenReturn(saved);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1));

        verify(locationService).addLocation(any(Location.class));
    }

    @Test
    void updateLocation_returnsOk() throws Exception {
        Location updated = new Location(2L, 37.98, 23.72, "Updated", null, "CAFE", true, false, 5, List.of());
        when(locationService.updateLocation(2L, updated)).thenReturn(updated);

        mockMvc.perform(put("/api/locations/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteLocation_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/locations/3"))
                .andExpect(status().isNoContent());

        verify(locationService).deleteLocation(3L);
    }
}
