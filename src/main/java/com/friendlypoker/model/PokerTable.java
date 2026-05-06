package com.friendlypoker.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "poker_tables")
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PokerTable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(nullable = false)
    private String name;

    private int smallBlind;
    private int bigBlind;
    private int minPlayers;
    private int maxPlayers;
    private int startingChips;
    private int actionTimeoutSecs;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GameVariant variant = GameVariant.TEXAS_HOLDEM;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TableStatus status = TableStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    private Instant createdAt;
}
