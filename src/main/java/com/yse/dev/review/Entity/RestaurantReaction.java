package com.yse.dev.review.Entity;

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
    name = "restaurant_reactions",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"place_id", "username"}
        )
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private String placeId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String reactionType;

    @Column(nullable = true)
    private String restaurantName;

    @Column(nullable = true)
    private String restaurantAddress;

    @Column(nullable = true)
    private String restaurantUrl;
}