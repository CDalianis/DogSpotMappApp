package com.example.mapstest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    private Long id;
    private double lat;
    private double lng;
    private String name;
    private String notes;
    private String category; // PARK, VET, GROOMING, CAFE, WATER, BAGS, OTHER
    private Boolean visited;
    private Boolean favorite;
    private Integer rating; // 1-5 (nullable)
    private List<String> photos; // data URLs (in-memory only)
}