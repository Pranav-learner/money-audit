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
    
    List<Saving> findTop5ByUserOrderBySavingDateDesc(User user);

    Optional<Saving> findByIdAndUser(UUID id, User user);

    @Query("SELECT SUM(s.amount) FROM Saving s WHERE s.user = :user")
    BigDecimal sumByUser(@Param("user") User user);

    @Query("""
        SELECT SUM(s.amount) FROM Saving s 
        WHERE s.user = :user 
        AND MONTH(s.savingDate) = :month 
        AND YEAR(s.savingDate) = :year
    """)
    BigDecimal sumByUserAndMonthAndYear(
            @Param("user") User user,
            @Param("month") int month,
            @Param("year") int year
    );
 
    @Query("""
        SELECT 
            MONTH(s.savingDate),
            SUM(s.amount)
        FROM Saving s
        WHERE s.user = :user
        AND YEAR(s.savingDate) = :year
        GROUP BY MONTH(s.savingDate)
        ORDER BY MONTH(s.savingDate)
    """)
    List<Object[]> getMonthlyTrend(
            @Param("user") User user,
            @Param("year") int year
    );

}

