package com.Pranav.finance_tracker.savings.repository;

import com.Pranav.finance_tracker.savings.entity.Saving;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingRepository extends JpaRepository<Saving, UUID> {

    List<Saving> findByUser(User user);

    Optional<Saving> findByIdAndUser(UUID id, User user);

    @Query("SELECT SUM(s.amount) FROM Saving s WHERE s.user = :user")
    BigDecimal sumByUser(@Param("user") User user);

    @Query("""
        SELECT SUM(s.amount) FROM Saving s 
        WHERE s.user = :user 
        AND FUNCTION('MONTH', s.savingDate) = :month 
        AND FUNCTION('YEAR', s.savingDate) = :year
    """)
    BigDecimal sumByUserAndMonthAndYear(
            @Param("user") User user,
            @Param("month") int month,
            @Param("year") int year
    );

    @Query("""
        SELECT new com.Pranav.finance_tracker.analytics.dto.SavingTrendItem(
            FUNCTION('MONTH', s.savingDate),
            SUM(s.amount)
        )
        FROM Saving s
        WHERE s.user = :user
        AND FUNCTION('YEAR', s.savingDate) = :year
        GROUP BY FUNCTION('MONTH', s.savingDate)
        ORDER BY FUNCTION('MONTH', s.savingDate)
    """)
    List<com.Pranav.finance_tracker.analytics.dto.SavingTrendItem> getMonthlyTrend(
            @Param("user") User user,
            @Param("year") int year
    );

}

