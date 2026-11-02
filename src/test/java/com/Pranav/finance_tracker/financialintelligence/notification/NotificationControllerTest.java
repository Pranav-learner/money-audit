package com.Pranav.finance_tracker.financialintelligence.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Test
    @DisplayName("GET /api/notifications returns list of user notifications")
    void getNotificationsReturnsList() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationResponse resp = NotificationResponse.builder()
                .id(id)
                .title("Budget Alert")
                .body("You have spent 85% of Dining budget")
                .type("BUDGET_EXCEEDED")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationService.getNotifications(false)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/notifications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].title").value("Budget Alert"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns unread count")
    void getUnreadCountReturnsCount() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read marks notification as read")
    void markAsReadUpdatesStatus() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationResponse resp = NotificationResponse.builder()
                .id(id)
                .title("Alert")
                .body("Content")
                .type("INSIGHT")
                .read(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationService.markAsRead(eq(id))).thenReturn(resp);

        mockMvc.perform(put("/api/notifications/" + id + "/read").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @DisplayName("PUT /api/notifications/read-all marks all notifications read")
    void markAllReadUpdatesAll() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(5);

        mockMvc.perform(put("/api/notifications/read-all").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedCount").value(5));
    }
}
