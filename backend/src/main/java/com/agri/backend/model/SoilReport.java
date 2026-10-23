package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * A lab soil-test report uploaded by the farmer (PDF/image/scanned copy),
 * plus the key values extracted from it (entered by the farmer or parsed).
 * Feeding these real, lab-measured values into the recommendation engine
 * alongside the ISRIC SoilGrids satellite estimate is what actually improves
 * accuracy - SoilGrids alone is a coarse global estimate.
 */
@Entity
@Table(name = "soil_reports")
@Data
public class SoilReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long farmerId;

    private String fileName;
    private String storedPath;      // where the uploaded file lives on disk/bucket
    private String contentType;

    // Key values from the report (nullable - farmer may only upload the file
    // and not transcribe every field; parsing the PDF automatically is a
    // further improvement, see FarmDataController notes)
    private Double phLevel;
    private Double nitrogenKgPerHa;
    private Double phosphorusKgPerHa;
    private Double potassiumKgPerHa;
    private Double organicCarbonPercent;
    private String soilTexture;     // e.g. Sandy Loam, Clay, Red Loam
    private String labName;
    private LocalDateTime testedOn;
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
