package com.example.attendance.paidleave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaidLeaveRejectRequest(
        @NotBlank @Size(max = 500) String reason,
        @NotNull Long version
) {
}
