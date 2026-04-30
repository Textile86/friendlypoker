package com.friendlypoker.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "table_seats",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"table_id", "user_id"}),
            @UniqueConstraint(columnNames = {"table_id", "seat_index"})
        })
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TableSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private PokerTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int seatIndex;

    @CreationTimestamp
    private Instant joinedAt;
}
