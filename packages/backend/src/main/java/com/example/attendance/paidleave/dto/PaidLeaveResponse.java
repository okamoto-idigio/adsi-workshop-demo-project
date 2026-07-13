package com.example.attendance.paidleave.dto;

import com.example.attendance.paidleave.entity.LeaveRequestStatus;
import com.example.attendance.paidleave.entity.LeaveType;
import com.example.attendance.paidleave.entity.PaidLeaveRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaidLeaveResponse(
        UUID id,
        UUID requesterId,
        String requesterName,
        UUID approverId,
        String approverName,
        LocalDate startDate,
        LocalDate endDate,
        LeaveType leaveType,
        BigDecimal leaveDays,
        LeaveRequestStatus status,
        String rejectReason,
        Long version,
        Instant createdAt
) {
    public static PaidLeaveResponse from(PaidLeaveRequest entity, BigDecimal leaveDays) {
        return new PaidLeaveResponse(
                entity.getId(),
                entity.getRequester().getId(),
                entity.getRequester().getName(),
                entity.getApprover() != null ? entity.getApprover().getId() : null,
                entity.getApprover() != null ? entity.getApprover().getName() : null,
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getLeaveType(),
                leaveDays,
                entity.getStatus(),
                entity.getRejectReason(),
                entity.getVersion(),
                entity.getCreatedAt()
        );
    }
}
