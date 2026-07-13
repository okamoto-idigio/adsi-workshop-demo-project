package com.example.attendance.paidleave.dto;

import com.example.attendance.paidleave.entity.LeaveRequestStatus;
import com.example.attendance.paidleave.entity.LeaveType;
import com.example.attendance.paidleave.entity.PaidLeaveRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PendingLeaveResponse(
        UUID id,
        UUID requesterId,
        String requesterName,
        LocalDate startDate,
        LocalDate endDate,
        LeaveType leaveType,
        BigDecimal leaveDays,
        LeaveRequestStatus status,
        Long version,
        Instant createdAt
) {
    public static PendingLeaveResponse from(PaidLeaveRequest entity, BigDecimal leaveDays) {
        return new PendingLeaveResponse(
                entity.getId(),
                entity.getRequester().getId(),
                entity.getRequester().getName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getLeaveType(),
                leaveDays,
                entity.getStatus(),
                entity.getVersion(),
                entity.getCreatedAt()
        );
    }
}
