"use client";

import { useState } from "react";
import { type Column, DataTable } from "@/components/DataTable";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import type { DailyAttendanceResponse } from "./attendance-api";
import { formatDate, formatMinutes, formatTime } from "./format";
import { MEMO_MAX_LENGTH, validateMemo } from "./memo-validation";
import { useUpdateMemo } from "./useAttendance";

function firstClockIn(day: DailyAttendanceResponse): string {
  const record = day.records[0];
  return record ? formatTime(record.clockIn) : "--:--";
}

function lastClockOut(day: DailyAttendanceResponse): string {
  const last = day.records[day.records.length - 1];
  return last?.clockOut ? formatTime(last.clockOut) : "--:--";
}

function hasCorrected(day: DailyAttendanceResponse): boolean {
  return day.records.some((r) => r.corrected);
}

function getMemo(day: DailyAttendanceResponse): string | null {
  const record = day.records[0];
  return record?.memo ?? null;
}

function getRecordId(day: DailyAttendanceResponse): string | null {
  const record = day.records[0];
  return record?.id ?? null;
}

function InlineMemoCell({ day }: { day: DailyAttendanceResponse }) {
  const recordId = getRecordId(day);
  const currentMemo = getMemo(day);
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState(currentMemo ?? "");
  const [memoError, setMemoError] = useState<string | null>(null);
  const updateMemoMutation = useUpdateMemo();

  if (!recordId) return <span className="text-muted-foreground">-</span>;

  const handleChange = (value: string) => {
    setEditValue(value);
    setMemoError(validateMemo(value));
  };

  const handleSave = () => {
    if (validateMemo(editValue)) {
      setEditValue(currentMemo ?? "");
      setMemoError(null);
      setIsEditing(false);
      return;
    }
    const newMemo = editValue.trim() || null;
    if (newMemo !== currentMemo) {
      updateMemoMutation.mutate({ recordId, memo: newMemo });
    }
    setMemoError(null);
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <div className="space-y-1">
        <Input
          type="text"
          value={editValue}
          onChange={(e) => handleChange(e.target.value)}
          maxLength={MEMO_MAX_LENGTH}
          className={`h-7 text-sm ${memoError ? "border-red-500" : ""}`}
          autoFocus
          onBlur={handleSave}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              (e.target as HTMLInputElement).blur();
            }
            if (e.key === "Escape") {
              setEditValue(currentMemo ?? "");
              setMemoError(null);
              setIsEditing(false);
            }
          }}
        />
        {memoError && <p className="text-xs text-red-500">{memoError}</p>}
      </div>
    );
  }

  return (
    <button
      type="button"
      onClick={() => {
        setEditValue(currentMemo ?? "");
        setIsEditing(true);
      }}
      className="text-left w-full min-h-[1.75rem] px-1 rounded hover:bg-muted transition-colors"
    >
      {currentMemo || <span className="text-muted-foreground">-</span>}
    </button>
  );
}

interface AttendanceTableProps {
  days: DailyAttendanceResponse[];
}

export function AttendanceTable({ days }: AttendanceTableProps) {
  const columns: Column<DailyAttendanceResponse>[] = [
    {
      key: "date",
      header: "日付",
      render: (day) => formatDate(day.date),
    },
    {
      key: "clockIn",
      header: "出勤",
      render: (day) => firstClockIn(day),
    },
    {
      key: "clockOut",
      header: "退勤",
      render: (day) => lastClockOut(day),
    },
    {
      key: "workMinutes",
      header: "勤務時間",
      render: (day) => (day.workMinutes > 0 ? formatMinutes(day.workMinutes) : "-"),
    },
    {
      key: "breakMinutes",
      header: "休憩",
      render: (day) => (day.breakMinutes > 0 ? formatMinutes(day.breakMinutes) : "-"),
    },
    {
      key: "overtimeMinutes",
      header: "残業",
      render: (day) => (day.overtimeMinutes > 0 ? formatMinutes(day.overtimeMinutes) : "-"),
    },
    {
      key: "memo",
      header: "備考",
      render: (day) => <InlineMemoCell day={day} />,
    },
    {
      key: "corrected",
      header: "",
      render: (day) => (hasCorrected(day) ? <Badge variant="outline">修正</Badge> : null),
    },
  ];

  return (
    <DataTable<DailyAttendanceResponse & Record<string, unknown>>
      columns={columns as Column<DailyAttendanceResponse & Record<string, unknown>>[]}
      data={days as (DailyAttendanceResponse & Record<string, unknown>)[]}
      rowKey={(item) => item.date}
      emptyMessage="勤怠データがありません"
    />
  );
}
