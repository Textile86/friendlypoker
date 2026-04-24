package com.friendlypoker.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table (name = "club_members", uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "user_id"}))
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClubMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubRole role;

    @CreationTimestamp
    private Instant joinedAt;
}
