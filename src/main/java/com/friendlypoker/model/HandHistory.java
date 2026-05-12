package com.friendlypoker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "hand_history")
@Getter
@Setter
public class HandHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private PokerTable table;

    private long handNumber;

    private Long winnerUserId;

    private int potAmount;

    @CreationTimestamp
    private Instant playedAt;
}
