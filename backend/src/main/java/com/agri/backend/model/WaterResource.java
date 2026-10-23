package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Water availability for the farmer's land - used to filter out crops that
 * need irrigation the farmer doesn't have (e.g. paddy needs standing water;
 * a farmer with only a borewell and low yield shouldn't be pointed at it).
 */
@Entity
@Table(name = "water_resources")
@Data
public class WaterResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long farmerId;

    private String source;          // BOREWELL, OPEN_WELL, CANAL, RAINFED_ONLY, MUNICIPAL, POND_TANK
    private Double availabilityLitersPerDay;
    private String irrigationMethod;   // DRIP, SPRINKLER, FLOOD, MANUAL
    private Boolean waterTestedForQuality;
    private Double waterPh;
    private Double salinityEc;         // EC in dS/m - high salinity rules out salt-sensitive crops
    private String notes;
}
