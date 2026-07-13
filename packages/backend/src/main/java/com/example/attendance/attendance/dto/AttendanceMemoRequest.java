package com.example.attendance.attendance.dto;

import jakarta.validation.constraints.Size;

public record AttendanceMemoRequest(
    @Size(max = 20)
    String memo
) {
}
