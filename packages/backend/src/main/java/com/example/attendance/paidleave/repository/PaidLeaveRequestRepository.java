package com.example.attendance.paidleave.repository;

import com.example.attendance.paidleave.entity.LeaveRequestStatus;
import com.example.attendance.paidleave.entity.PaidLeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaidLeaveRequestRepository extends JpaRepository<PaidLeaveRequest, UUID> {

    List<PaidLeaveRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<PaidLeaveRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(
            UUID requesterId, LeaveRequestStatus status);

    List<PaidLeaveRequest> findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
            UUID departmentId, LeaveRequestStatus status);

    @Query("""
            SELECT p FROM PaidLeaveRequest p
            WHERE p.requester.id = :requesterId
            AND p.status IN :statuses
            AND p.startDate <= :endDate
            AND p.endDate >= :startDate
            """)
    List<PaidLeaveRequest> findOverlapping(
            @Param("requesterId") UUID requesterId,
            @Param("statuses") List<LeaveRequestStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
