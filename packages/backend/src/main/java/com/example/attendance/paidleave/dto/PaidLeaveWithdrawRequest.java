package com.example.attendance.paidleave.dto;

import jakarta.validation.constraints.NotNull;

public record PaidLeaveWithdrawRequest(
        @NotNull Long version
) {
}
