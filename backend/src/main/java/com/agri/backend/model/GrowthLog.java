package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "growth_logs")
@Data
public class GrowthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @ManyToOne
    @JoinColumn(name = "crop_guide_id")
    private CropGuide cropGuide;

    private LocalDate sowingDate;
    private String currentStage;   // LAND_PREP, SOWN, GROWING, FLOWERING, HARVEST_READY, HARVESTED
    private String notes;
}
