package com.friendlypoker.repository;

import com.friendlypoker.model.TableSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableSeatRepository extends JpaRepository<TableSeat, Long> {
    List<TableSeat> findByTableId(Long tableId);
    Optional<TableSeat> findByTableIdAndUserId(Long tableId, Long userId);
    boolean existsByTableIdAndUserId(Long tableId, Long userId);
    int countByTableId(Long tableId);
}
