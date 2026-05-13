package com.example.mapstest.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Location {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private double lat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private double lng;

    @NotBlank(message = "Name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Pattern(regexp = "PARK|VET|GROOMING|CAFE|WATER|BAGS|OTHER", message = "Unknown category")
    private String category;

    private Boolean visited;
    private Boolean favorite;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "location_photos", joinColumns = @JoinColumn(name = "location_id"))
    @Column(name = "data_url", columnDefinition = "TEXT")
    @OrderColumn(name = "photo_order")
    private List<String> photos = new ArrayList<>();
}
