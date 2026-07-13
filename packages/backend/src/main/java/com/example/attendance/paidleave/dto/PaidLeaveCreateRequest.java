package com.example.attendance.paidleave.dto;

import com.example.attendance.paidleave.entity.LeaveType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PaidLeaveCreateRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull LeaveType leaveType
) {
}
