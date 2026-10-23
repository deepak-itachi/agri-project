package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Maps a crop (by name, matching CropGuide.cropName) to a representative
 * image shown in the recommendation results and crop guide page. Kept as
 * its own table (rather than a column on CropGuide) so multiple images per
 * crop, or swapping the dataset/source, doesn't require a CropGuide migration.
 */
@Entity
@Table(name = "crop_images")
@Data
public class CropImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cropName;     // must match CropGuide.cropName

    @Column(nullable = false)
    private String imageUrl;     // e.g. /images/crops/tomato.jpg or a CDN URL
    private String sourceCredit; // attribution if the image isn't your own
}
