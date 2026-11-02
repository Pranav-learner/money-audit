package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.group.dto.CreateGroupExpenseRequest;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.group.entity.GroupMember;
import com.Pranav.finance_tracker.group.enums.SplitType;
import com.Pranav.finance_tracker.group.repository.GroupExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupExpenseEqualSplitTest {

    @Mock
    private GroupExpenseRepository groupExpenseRepository;

    @Mock
    private GroupExpenseSplitRepository groupExpenseSplitRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private GroupExpenseService groupExpenseService;

    @Captor
    private ArgumentCaptor<List<GroupExpenseSplit>> splitCaptor;

    private User alice;
    private User bob;
    private User charlie;
    private Group group;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(UUID.randomUUID()).name("Alice").email("alice@test.com").build();
        bob = User.builder().id(UUID.randomUUID()).name("Bob").email("bob@test.com").build();
        charlie = User.builder().id(UUID.randomUUID()).name("Charlie").email("charlie@test.com").build();

        groupId = UUID.randomUUID();
        group = Group.builder().id(groupId).name("Tripmates").build();

        when(securityUtils.getCurrentUser()).thenReturn(alice);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupAndUser(group, alice)).thenReturn(true);

        GroupMember m1 = GroupMember.builder().group(group).user(alice).build();
        GroupMember m2 = GroupMember.builder().group(group).user(bob).build();
        GroupMember m3 = GroupMember.builder().group(group).user(charlie).build();
        when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(m1, m2, m3));

        when(groupExpenseRepository.save(any(GroupExpense.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Splitting ₹100.00 equally among 3 participants sums to exactly ₹100.00 without penny loss")
    void split100AmongThreeEqualsExactly100() {
        CreateGroupExpenseRequest req = new CreateGroupExpenseRequest();
        req.setGroupId(groupId);
        req.setTitle("Group Dinner");
        req.setTotalAmount(new BigDecimal("100.00"));
        req.setExpenseDate(LocalDate.now());
        req.setSplitType(SplitType.EQUAL);

        groupExpenseService.createGroupExpense(req);

        verify(groupExpenseSplitRepository).saveAll(splitCaptor.capture());
        List<GroupExpenseSplit> splits = splitCaptor.getValue();

        assertThat(splits).hasSize(3);

        BigDecimal sum = splits.stream()
                .map(GroupExpenseSplit::getAmountOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Crucial invariant: sum must equal 100.00 exactly
        assertThat(sum).isEqualByComparingTo(new BigDecimal("100.00"));

        // First gets 33.34, second gets 33.33, third gets 33.33
        assertThat(splits.get(0).getAmountOwed()).isEqualByComparingTo(new BigDecimal("33.34"));
        assertThat(splits.get(1).getAmountOwed()).isEqualByComparingTo(new BigDecimal("33.33"));
        assertThat(splits.get(2).getAmountOwed()).isEqualByComparingTo(new BigDecimal("33.33"));
    }
}
