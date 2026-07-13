package com.example.attendance.paidleave.service;

import com.example.attendance.paidleave.dto.PaidLeaveCreateRequest;
import com.example.attendance.paidleave.dto.PaidLeaveResponse;
import com.example.attendance.paidleave.dto.PendingLeaveResponse;
import com.example.attendance.paidleave.entity.LeaveRequestStatus;

import java.util.List;
import java.util.UUID;

public interface PaidLeaveService {

    PaidLeaveResponse create(UUID requesterId, PaidLeaveCreateRequest request);

    List<PaidLeaveResponse> findByRequester(UUID requesterId, LeaveRequestStatus status);

    PaidLeaveResponse withdraw(UUID leaveId, UUID requesterId, Long version);

    List<PendingLeaveResponse> findPending(UUID managerId);

    PaidLeaveResponse approve(UUID leaveId, UUID approverId, Long version);

    PaidLeaveResponse reject(UUID leaveId, UUID approverId, String reason, Long version);
}
