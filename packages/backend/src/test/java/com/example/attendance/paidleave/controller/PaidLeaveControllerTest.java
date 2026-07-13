package com.example.attendance.paidleave.controller;

import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import com.example.attendance.paidleave.dto.PaidLeaveResponse;
import com.example.attendance.paidleave.dto.PendingLeaveResponse;
import com.example.attendance.paidleave.entity.LeaveRequestStatus;
import com.example.attendance.paidleave.entity.LeaveType;
import com.example.attendance.paidleave.service.PaidLeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = PaidLeaveController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(PaidLeaveControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class PaidLeaveControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaidLeaveService paidLeaveService;

    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LEAVE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Test
    @DisplayName("POST /api/paid-leaves returns 201")
    void create_validRequest_returns201() throws Exception {
        var response = new PaidLeaveResponse(LEAVE_ID, EMPLOYEE_ID, "Tanaka", null, null,
                LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY,
                new BigDecimal("1.0"), LeaveRequestStatus.PENDING, null, 0L, Instant.parse("2026-07-13T02:00:00Z"));
        when(paidLeaveService.create(eq(EMPLOYEE_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/paid-leaves").param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-07-14\",\"endDate\":\"2026-07-14\",\"leaveType\":\"FULL_DAY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/paid-leaves validation error returns 400")
    void create_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/paid-leaves").param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":null,\"endDate\":null,\"leaveType\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/paid-leaves returns 200")
    void findByRequester_returns200() throws Exception {
        var response = new PaidLeaveResponse(LEAVE_ID, EMPLOYEE_ID, "Tanaka", null, null,
                LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY,
                new BigDecimal("1.0"), LeaveRequestStatus.PENDING, null, 0L, Instant.parse("2026-07-13T02:00:00Z"));
        when(paidLeaveService.findByRequester(eq(EMPLOYEE_ID), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/paid-leaves").param("requesterId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH withdraw returns 200")
    void withdraw_returns200() throws Exception {
        var response = new PaidLeaveResponse(LEAVE_ID, EMPLOYEE_ID, "Tanaka", null, null,
                LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY,
                new BigDecimal("1.0"), LeaveRequestStatus.WITHDRAWN, null, 1L, Instant.parse("2026-07-13T02:00:00Z"));
        when(paidLeaveService.withdraw(LEAVE_ID, EMPLOYEE_ID, 0L)).thenReturn(response);

        mockMvc.perform(patch("/api/paid-leaves/{id}/withdraw", LEAVE_ID)
                        .param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    @Test
    @DisplayName("GET /api/paid-leaves/pending returns 200")
    void findPending_returns200() throws Exception {
        var response = new PendingLeaveResponse(LEAVE_ID, EMPLOYEE_ID, "Tanaka",
                LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY,
                new BigDecimal("1.0"), LeaveRequestStatus.PENDING, 0L, Instant.parse("2026-07-13T02:00:00Z"));
        when(paidLeaveService.findPending(MANAGER_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/paid-leaves/pending").param("managerId", MANAGER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requesterName").value("Tanaka"));
    }

    @Test
    @DisplayName("PATCH approve returns 200")
    void approve_returns200() throws Exception {
        var response = new PaidLeaveResponse(LEAVE_ID, EMPLOYEE_ID, "Tanaka", MANAGER_ID, "Sato",
                LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY,
                new BigDecimal("1.0"), LeaveRequestStatus.APPROVED, null, 1L, Instant.parse("2026-07-13T02:00:00Z"));
        when(paidLeaveService.approve(LEAVE_ID, MANAGER_ID, 0L)).thenReturn(response);

        mockMvc.perform(patch("/api/paid-leaves/{id}/approve", LEAVE_ID)
                        .param("approverId", MANAGER_ID.toString()).param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH reject returns 200")
    void reject_returns200() throws Exception {
        var response = new PaidLeaveResponse(LEAVE_ID, EMPLOYEE_ID, "Tanaka", MANAGER_ID, "Sato",
                LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY,
                new BigDecimal("1.0"), LeaveRequestStatus.REJECTED, "Busy", 1L, Instant.parse("2026-07-13T02:00:00Z"));
        when(paidLeaveService.reject(eq(LEAVE_ID), eq(MANAGER_ID), eq("Busy"), eq(0L))).thenReturn(response);

        mockMvc.perform(patch("/api/paid-leaves/{id}/reject", LEAVE_ID)
                        .param("approverId", MANAGER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Busy\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PATCH reject with empty reason returns 400")
    void reject_emptyReason_returns400() throws Exception {
        mockMvc.perform(patch("/api/paid-leaves/{id}/reject", LEAVE_ID)
                        .param("approverId", MANAGER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\",\"version\":0}"))
                .andExpect(status().isBadRequest());
    }
}
