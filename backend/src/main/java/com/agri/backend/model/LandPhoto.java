package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Photo(s) of the actual land/plot. Used today to show the farmer their own
 * land on the dashboard; the stored path is also the hook point for a future
 * vision-based check (existing crop cover, shade, drainage, encroachment)
 * without changing the API shape later.
 */
@Entity
@Table(name = "land_photos")
@Data
public class LandPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long farmerId;
    private String fileName;
    private String storedPath;
    private String contentType;
    private String caption;   // e.g. "north corner", "existing well"
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
