package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "farmers")
@Data
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String location;

    private Double landSizeSqft;
    private String landType;        // OPEN_FIELD, TERRACE, BACKYARD, ROOFTOP_CONTAINER
    private String experienceLevel; // BEGINNER, HOBBYIST, SERIOUS
    private Double budgetInRupees;

    private String scale;                 // result from SOAP classification
    private String suggestedFarmingMethod;
}
