package com.yse.dev.TourReview.Reaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "tour_reactions",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"spot_id", "username"}
        )
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_id", nullable = false)
    private String spotId;

    @Column(nullable = false)
    private String username;

    private String reactionType;

    @Column(nullable = true)
    private String spotName;
}