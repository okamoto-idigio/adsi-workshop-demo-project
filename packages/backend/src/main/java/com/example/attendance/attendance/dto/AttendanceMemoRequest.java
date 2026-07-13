package com.example.attendance.attendance.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AttendanceMemoRequest(
    @Size(max = 20, message = "メモは20文字以内で入力してください")
    @Pattern(regexp = "^[^\\n\\r<>]*$", message = "改行・HTMLタグは使用できません")
    String memo
) {
}
