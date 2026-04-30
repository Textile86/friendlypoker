package com.friendlypoker.repository;

import com.friendlypoker.model.PokerTable;
import com.friendlypoker.model.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PokerTableRepository extends JpaRepository<PokerTable, Long> {
    List<PokerTable> findByClubId(Long clubId);
    List<PokerTable> findByClubIdAndStatus(Long clubId, TableStatus status);
}
