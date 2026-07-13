package com.example.attendance.paidleave.service;

import com.example.attendance.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.paidleave.dto.PaidLeaveCreateRequest;
import com.example.attendance.paidleave.dto.PaidLeaveResponse;
import com.example.attendance.paidleave.dto.PendingLeaveResponse;
import com.example.attendance.paidleave.entity.LeaveRequestStatus;
import com.example.attendance.paidleave.entity.LeaveType;
import com.example.attendance.paidleave.entity.PaidLeaveRequest;
import com.example.attendance.paidleave.repository.PaidLeaveRequestRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PaidLeaveServiceImpl implements PaidLeaveService {

    private final PaidLeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final Clock clock;

    public PaidLeaveServiceImpl(
            PaidLeaveRequestRepository leaveRepository,
            EmployeeRepository employeeRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            Clock clock) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaidLeaveResponse create(UUID requesterId, PaidLeaveCreateRequest request) {
        var requester = findEmployeeOrThrow(requesterId);
        validateCreateRequest(requester, request);

        var leave = PaidLeaveRequest.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .requester(requester)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .leaveType(request.leaveType())
                .status(LeaveRequestStatus.PENDING)
                .build();

        var saved = leaveRepository.save(leave);
        log.info("Paid leave request created: id={}, requester={}", saved.getId(), requesterId);
        return PaidLeaveResponse.from(saved, calculateLeaveDays(saved));
    }

    @Override
    public List<PaidLeaveResponse> findByRequester(UUID requesterId, LeaveRequestStatus status) {
        List<PaidLeaveRequest> leaves;
        if (status != null) {
            leaves = leaveRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterId, status);
        } else {
            leaves = leaveRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        }
        return leaves.stream()
                .map(l -> PaidLeaveResponse.from(l, calculateLeaveDays(l)))
                .toList();
    }

    @Override
    @Transactional
    public PaidLeaveResponse withdraw(UUID leaveId, UUID requesterId, Long version) {
        var leave = findLeaveOrThrow(leaveId);

        if (!leave.getRequester().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the requester can withdraw the leave request");
        }
        if (leave.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING requests can be withdrawn");
        }
        validateVersion(leave, version);

        leave.setStatus(LeaveRequestStatus.WITHDRAWN);
        var saved = leaveRepository.save(leave);
        log.info("Paid leave request withdrawn: id={}, requester={}", leaveId, requesterId);
        return PaidLeaveResponse.from(saved, calculateLeaveDays(saved));
    }

    @Override
    public List<PendingLeaveResponse> findPending(UUID managerId) {
        var mgr = findEmployeeOrThrow(managerId);
        var departmentId = mgr.getDepartment().getId();
        var leaves = leaveRepository
                .findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                        departmentId, LeaveRequestStatus.PENDING);
        return leaves.stream()
                .map(l -> PendingLeaveResponse.from(l, calculateLeaveDays(l)))
                .toList();
    }

    @Override
    @Transactional
    public PaidLeaveResponse approve(UUID leaveId, UUID approverId, Long version) {
        var leave = findLeaveOrThrow(leaveId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(leave, approver);
        validateVersion(leave, version);

        leave.setStatus(LeaveRequestStatus.APPROVED);
        leave.setApprover(approver);

        var saved = leaveRepository.save(leave);
        log.info("Paid leave request approved: id={}, approver={}", leaveId, approverId);
        return PaidLeaveResponse.from(saved, calculateLeaveDays(saved));
    }

    @Override
    @Transactional
    public PaidLeaveResponse reject(UUID leaveId, UUID approverId, String reason, Long version) {
        var leave = findLeaveOrThrow(leaveId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(leave, approver);
        validateVersion(leave, version);

        leave.setStatus(LeaveRequestStatus.REJECTED);
        leave.setApprover(approver);
        leave.setRejectReason(reason);

        var saved = leaveRepository.save(leave);
        log.info("Paid leave request rejected: id={}, approver={}", leaveId, approverId);
        return PaidLeaveResponse.from(saved, calculateLeaveDays(saved));
    }

    private void validateCreateRequest(Employee requester, PaidLeaveCreateRequest request) {
        var today = LocalDate.now(clock);

        if (!request.startDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be after today");
        }
        if (request.leaveType() != LeaveType.FULL_DAY && !request.startDate().equals(request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Half-day leave must be for a single day");
        }

        var workingDays = getWorkingDays(request.startDate(), request.endDate());
        if (workingDays.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No working days in the specified date range");
        }

        var overlapping = leaveRepository.findOverlapping(
                requester.getId(),
                List.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED),
                request.startDate(), request.endDate());
        if (!overlapping.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already a leave request for the specified dates");
        }

        var existingRecords = attendanceRecordRepository.findByEmployeeIdAndWorkDateIn(requester.getId(), workingDays);
        if (!existingRecords.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There are already attendance records for the specified dates");
        }
    }

    private void validateApprover(PaidLeaveRequest leave, Employee approver) {
        var requesterDeptId = leave.getRequester().getDepartment().getId();
        var approverDeptId = approver.getDepartment().getId();
        if (!approverDeptId.equals(requesterDeptId) || !approver.isManager()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the department manager can approve/reject leave requests");
        }
    }

    private void validateVersion(PaidLeaveRequest leave, Long version) {
        if (!leave.getVersion().equals(version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The leave request was modified by another user. Please refresh and try again.");
        }
    }

    private BigDecimal calculateLeaveDays(PaidLeaveRequest leave) {
        if (leave.getLeaveType() != LeaveType.FULL_DAY) {
            return new BigDecimal("0.5");
        }
        return new BigDecimal(getWorkingDays(leave.getStartDate(), leave.getEndDate()).size());
    }

    private List<LocalDate> getWorkingDays(LocalDate start, LocalDate end) {
        var days = new ArrayList<LocalDate>();
        var current = start;
        while (!current.isAfter(end)) {
            var dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                days.add(current);
            }
            current = current.plusDays(1);
        }
        return days;
    }

    private PaidLeaveRequest findLeaveOrThrow(UUID leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new EntityNotFoundException("PaidLeaveRequest with id '%s' was not found".formatted(leaveId)));
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee with id '%s' was not found".formatted(employeeId)));
    }
}
