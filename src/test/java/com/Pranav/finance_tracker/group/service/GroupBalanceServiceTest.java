package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.group.dto.BalanceSummaryResponse;
import com.Pranav.finance_tracker.group.dto.MemberBalanceDetail;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.group.entity.GroupMember;
import com.Pranav.finance_tracker.group.repository.GroupExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupBalanceServiceTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupExpenseRepository groupExpenseRepository;

    @Mock
    private GroupExpenseSplitRepository splitRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private GroupBalanceService groupBalanceService;

    private User alice;
    private User bob;
    private Group testGroup;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        alice = User.builder().id(UUID.randomUUID()).name("Alice").email("alice@test.com").build();
        bob = User.builder().id(UUID.randomUUID()).name("Bob").email("bob@test.com").build();

        testGroup = Group.builder().id(groupId).name("Apartment").build();

        GroupMember m1 = GroupMember.builder().group(testGroup).user(alice).build();
        GroupMember m2 = GroupMember.builder().group(testGroup).user(bob).build();

        when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(m1, m2));
    }

    @Test
    @DisplayName("Bob owes Alice ₹500 initially; paying ₹200 reduces debt to ₹300, not ₹700 (settlement sign fix)")
    void settlementReducesDebtCorrectlyForOwer() {
        // Alice paid ₹1000 for dinner
        GroupExpense expense = GroupExpense.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .paidBy(alice)
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        // Bob owes ₹500
        GroupExpenseSplit bobSplit = GroupExpenseSplit.builder()
                .expense(expense)
                .user(bob)
                .amountOwed(new BigDecimal("500.00"))
                .build();

        when(groupExpenseRepository.findByGroupId(groupId)).thenReturn(List.of(expense));
        when(splitRepository.findByExpenseGroupId(groupId)).thenReturn(List.of(bobSplit));

        // Bob paid Alice ₹200 as settlement
        Payment partialPayment = Payment.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .fromUser(bob)
                .toUser(alice)
                .amount(new BigDecimal("200.00"))
                .build();

        when(paymentRepository.findByGroupId(groupId)).thenReturn(List.of(partialPayment));

        BalanceSummaryResponse response = groupBalanceService.getBalanceSummary(groupId, bob);

        assertThat(response).isNotNull();
        // Bob's net balance should be -300 (owed 500, paid 200 -> net -300)
        assertThat(response.getNetBalance()).isEqualByComparingTo(new BigDecimal("-300.00"));

        MemberBalanceDetail aliceDetail = response.getBalances().stream()
                .filter(b -> b.getUserId().equals(alice.getId()))
                .findFirst().orElseThrow();

        assertThat(aliceDetail.getType()).isEqualTo(MemberBalanceDetail.BalanceType.YOU_OWE);
        assertThat(aliceDetail.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Full settlement: Bob pays Alice remaining ₹500; Bob's balance is SETTLED at 0")
    void fullSettlementResultsInZeroDebt() {
        GroupExpense expense = GroupExpense.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .paidBy(alice)
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        GroupExpenseSplit bobSplit = GroupExpenseSplit.builder()
                .expense(expense)
                .user(bob)
                .amountOwed(new BigDecimal("500.00"))
                .build();

        when(groupExpenseRepository.findByGroupId(groupId)).thenReturn(List.of(expense));
        when(splitRepository.findByExpenseGroupId(groupId)).thenReturn(List.of(bobSplit));

        Payment fullPayment = Payment.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .fromUser(bob)
                .toUser(alice)
                .amount(new BigDecimal("500.00"))
                .build();

        when(paymentRepository.findByGroupId(groupId)).thenReturn(List.of(fullPayment));

        BalanceSummaryResponse response = groupBalanceService.getBalanceSummary(groupId, bob);

        assertThat(response.getNetBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        MemberBalanceDetail aliceDetail = response.getBalances().stream()
                .filter(b -> b.getUserId().equals(alice.getId()))
                .findFirst().orElseThrow();

        assertThat(aliceDetail.getType()).isEqualTo(MemberBalanceDetail.BalanceType.SETTLED);
        assertThat(aliceDetail.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Alice view: Alice is owed ₹500, receives ₹200, is now owed ₹300")
    void settlementReducesAmountOwedToPayer() {
        GroupExpense expense = GroupExpense.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .paidBy(alice)
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        GroupExpenseSplit bobSplit = GroupExpenseSplit.builder()
                .expense(expense)
                .user(bob)
                .amountOwed(new BigDecimal("500.00"))
                .build();

        when(groupExpenseRepository.findByGroupId(groupId)).thenReturn(List.of(expense));
        when(splitRepository.findByExpenseGroupId(groupId)).thenReturn(List.of(bobSplit));

        Payment partialPayment = Payment.builder()
                .id(UUID.randomUUID())
                .group(testGroup)
                .fromUser(bob)
                .toUser(alice)
                .amount(new BigDecimal("200.00"))
                .build();

        when(paymentRepository.findByGroupId(groupId)).thenReturn(List.of(partialPayment));

        BalanceSummaryResponse response = groupBalanceService.getBalanceSummary(groupId, alice);

        assertThat(response.getNetBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        MemberBalanceDetail bobDetail = response.getBalances().stream()
                .filter(b -> b.getUserId().equals(bob.getId()))
                .findFirst().orElseThrow();

        assertThat(bobDetail.getType()).isEqualTo(MemberBalanceDetail.BalanceType.YOU_ARE_OWED);
        assertThat(bobDetail.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }
}
