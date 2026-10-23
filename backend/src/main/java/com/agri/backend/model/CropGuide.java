package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "crop_guides")
@Data
public class CropGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cropName;
    private String suitableLandTypes;   // comma separated: TERRACE,BACKYARD
    private String suitableScale;       // SMALL_SCALE, MEDIUM_SCALE, LARGE_SCALE
    private Integer durationDays;       // total crop cycle length

    @Column(length = 2000)
    private String landPreparationSteps;
    @Column(length = 2000)
    private String sowingInstructions;
    @Column(length = 2000)
    private String wateringSchedule;
    @Column(length = 2000)
    private String fertilizerPlan;
    @Column(length = 2000)
    private String pestWatch;
    @Column(length = 2000)
    private String harvestGuide;

    private Double estimatedCostPerAcreOrUnit;
    private Double estimatedYieldPerAcreOrUnit; // in kg
}
