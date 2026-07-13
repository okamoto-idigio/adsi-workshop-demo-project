"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { LeaveType } from "./paid-leave-api";
import { useCreatePaidLeave } from "./usePaidLeaves";

function countWorkingDays(start: string, end: string): number {
  if (!start || !end) return 0;
  const startDate = new Date(start);
  const endDate = new Date(end);
  let count = 0;
  const current = new Date(startDate);
  while (current <= endDate) {
    const dow = current.getDay();
    if (dow !== 0 && dow !== 6) count++;
    current.setDate(current.getDate() + 1);
  }
  return count;
}

export function PaidLeaveForm() {
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [leaveType, setLeaveType] = useState<LeaveType>("FULL_DAY");
  const createMutation = useCreatePaidLeave();

  const isHalfDay = leaveType === "AM_HALF" || leaveType === "PM_HALF";
  const effectiveEndDate = isHalfDay ? startDate : endDate;
  const workingDays = countWorkingDays(startDate, effectiveEndDate);
  const leaveDays = isHalfDay ? 0.5 : workingDays;

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const minDate = tomorrow.toISOString().split("T")[0];

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!startDate) return;
    createMutation.mutate(
      { startDate, endDate: effectiveEndDate || startDate, leaveType },
      {
        onSuccess: () => {
          setStartDate("");
          setEndDate("");
          setLeaveType("FULL_DAY");
        },
      },
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border p-4">
      <h3 className="text-lg font-semibold">有給休暇申請</h3>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="space-y-2">
          <Label htmlFor="leaveType">休暇種別</Label>
          <Select value={leaveType} onValueChange={(v) => setLeaveType(v as LeaveType)}>
            <SelectTrigger id="leaveType"><SelectValue>{(value: string | null) => {
              const labels: Record<string, string> = { FULL_DAY: "全休", AM_HALF: "午前休", PM_HALF: "午後休" };
              return labels[value ?? ""] ?? "選択してください";
            }}</SelectValue></SelectTrigger>
            <SelectContent>
              <SelectItem value="FULL_DAY">全休</SelectItem>
              <SelectItem value="AM_HALF">午前休</SelectItem>
              <SelectItem value="PM_HALF">午後休</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="startDate">開始日</Label>
          <Input id="startDate" type="date" value={startDate} min={minDate} onChange={(e) => setStartDate(e.target.value)} required />
        </div>
        <div className="space-y-2">
          <Label htmlFor="endDate">終了日</Label>
          <Input id="endDate" type="date" value={isHalfDay ? startDate : endDate} min={startDate || minDate} onChange={(e) => setEndDate(e.target.value)} disabled={isHalfDay} required />
        </div>
      </div>
      {startDate && <p className="text-sm text-muted-foreground">申請日数: <span className="font-medium">{leaveDays}日</span>{!isHalfDay && workingDays > 0 && "（土日除く）"}</p>}
      <Button type="submit" disabled={createMutation.isPending || !startDate || leaveDays === 0}>{createMutation.isPending ? "送信中..." : "申請する"}</Button>
    </form>
  );
}
