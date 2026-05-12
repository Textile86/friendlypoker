package com.friendlypoker.repository;

import com.friendlypoker.model.HandHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HandHistoryRepository extends JpaRepository<HandHistory, Long> {
    List<HandHistory> findByTableIdOrderByHandNumberAsc(Long tableId);
}
