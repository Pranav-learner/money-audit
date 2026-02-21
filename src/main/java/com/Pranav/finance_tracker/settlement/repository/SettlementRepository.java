package com.Pranav.finance_tracker.settlement.repository;

import com.Pranav.finance_tracker.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    List<Settlement> findByGroupId(UUID groupId);
}
