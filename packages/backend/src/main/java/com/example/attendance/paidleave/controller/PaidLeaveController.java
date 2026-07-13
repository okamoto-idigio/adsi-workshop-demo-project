package com.example.attendance.paidleave.controller;

import com.example.attendance.paidleave.dto.PaidLeaveCreateRequest;
import com.example.attendance.paidleave.dto.PaidLeaveRejectRequest;
import com.example.attendance.paidleave.dto.PaidLeaveResponse;
import com.example.attendance.paidleave.dto.PaidLeaveWithdrawRequest;
import com.example.attendance.paidleave.dto.PendingLeaveResponse;
import com.example.attendance.paidleave.entity.LeaveRequestStatus;
import com.example.attendance.paidleave.service.PaidLeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/paid-leaves")
public class PaidLeaveController {

    private final PaidLeaveService paidLeaveService;

    public PaidLeaveController(PaidLeaveService paidLeaveService) {
        this.paidLeaveService = paidLeaveService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaidLeaveResponse create(
            @RequestParam UUID requesterId,
            @Valid @RequestBody PaidLeaveCreateRequest request) {
        return paidLeaveService.create(requesterId, request);
    }

    @GetMapping
    public List<PaidLeaveResponse> findByRequester(
            @RequestParam UUID requesterId,
            @RequestParam(required = false) LeaveRequestStatus status) {
        return paidLeaveService.findByRequester(requesterId, status);
    }

    @PatchMapping("/{id}/withdraw")
    public PaidLeaveResponse withdraw(
            @PathVariable UUID id,
            @RequestParam UUID requesterId,
            @Valid @RequestBody PaidLeaveWithdrawRequest request) {
        return paidLeaveService.withdraw(id, requesterId, request.version());
    }

    @GetMapping("/pending")
    public List<PendingLeaveResponse> findPending(@RequestParam UUID managerId) {
        return paidLeaveService.findPending(managerId);
    }

    @PatchMapping("/{id}/approve")
    public PaidLeaveResponse approve(
            @PathVariable UUID id,
            @RequestParam UUID approverId,
            @RequestParam Long version) {
        return paidLeaveService.approve(id, approverId, version);
    }

    @PatchMapping("/{id}/reject")
    public PaidLeaveResponse reject(
            @PathVariable UUID id,
            @RequestParam UUID approverId,
            @Valid @RequestBody PaidLeaveRejectRequest request) {
        return paidLeaveService.reject(id, approverId, request.reason(), request.version());
    }
}
