"use client";

import { LogIn, LogOut } from "lucide-react";
import { useEffect, useState } from "react";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { formatTime } from "./format";
import { MEMO_MAX_LENGTH, validateMemo } from "./memo-validation";
import { useClockIn, useClockOut, useTodayStatus } from "./useAttendance";

function CurrentTime() {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const timeStr = now.toLocaleTimeString("ja-JP", {
    timeZone: "Asia/Tokyo",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });

  const dateStr = now.toLocaleDateString("ja-JP", {
    timeZone: "Asia/Tokyo",
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
  });

  return (
    <div className="text-center">
      <p className="text-4xl font-bold tabular-nums tracking-tight">{timeStr}</p>
      <p className="text-sm text-muted-foreground mt-1">{dateStr}</p>
    </div>
  );
}

const STATUS_LABELS = {
  NOT_CLOCKED_IN: "未出勤",
  CLOCKED_IN: "勤務中",
  CLOCKED_OUT: "退勤済み",
} as const;

export function ClockButtons() {
  const { data: todayStatus, isLoading } = useTodayStatus();
  const clockInMutation = useClockIn();
  const clockOutMutation = useClockOut();
  const [memo, setMemo] = useState("");
  const [memoError, setMemoError] = useState<string | null>(null);

  const lastRecord = todayStatus?.records[todayStatus.records.length - 1];

  useEffect(() => {
    if (lastRecord?.memo) {
      setMemo(lastRecord.memo);
    }
  }, [lastRecord?.memo]);

  if (isLoading) {
    return (
      <div className="rounded-lg border p-6 space-y-4">
        <Skeleton className="h-12 w-48 mx-auto" />
        <Skeleton className="h-5 w-24 mx-auto" />
        <div className="flex justify-center gap-4">
          <Skeleton className="h-10 w-28" />
          <Skeleton className="h-10 w-28" />
        </div>
      </div>
    );
  }

  const status = todayStatus?.status ?? "NOT_CLOCKED_IN";
  const canClockIn = status !== "CLOCKED_IN";
  const canClockOut = status === "CLOCKED_IN";
  const isPending = clockInMutation.isPending || clockOutMutation.isPending;

  const handleMemoChange = (value: string) => {
    setMemo(value);
    setMemoError(validateMemo(value));
  };

  return (
    <div className="rounded-lg border p-6 space-y-4">
      <CurrentTime />
      <div className="flex items-center justify-center gap-2">
        <span className="text-sm text-muted-foreground">{STATUS_LABELS[status]}</span>
        {lastRecord && status === "CLOCKED_IN" && (
          <span className="text-sm text-muted-foreground">
            ({formatTime(lastRecord.clockIn)} ~)
          </span>
        )}
      </div>
      <div className="grid grid-cols-2 gap-4 max-w-md mx-auto">
        <button
          type="button"
          disabled={!canClockIn || isPending}
          onClick={() => clockInMutation.mutate(memo || undefined)}
          className="flex flex-col items-center justify-center gap-2 rounded-xl bg-blue-800 py-8 text-white transition-colors hover:bg-blue-900 active:bg-blue-950 disabled:bg-gray-200 disabled:text-gray-400"
        >
          <LogIn className="h-8 w-8" />
          <span className="text-lg font-bold">出勤</span>
        </button>
        <button
          type="button"
          disabled={!canClockOut || isPending}
          onClick={() => clockOutMutation.mutate(memo || undefined)}
          className="flex flex-col items-center justify-center gap-2 rounded-xl bg-orange-500 py-8 text-white transition-colors hover:bg-orange-600 active:bg-orange-700 disabled:bg-gray-200 disabled:text-gray-400"
        >
          <LogOut className="h-8 w-8" />
          <span className="text-lg font-bold">退勤</span>
        </button>
      </div>
      <div className="max-w-md mx-auto space-y-1">
        <Input
          type="text"
          value={memo}
          onChange={(e) => handleMemoChange(e.target.value)}
          maxLength={MEMO_MAX_LENGTH}
          className={memoError ? "border-red-500" : ""}
        />
        {memoError && <p className="text-sm text-red-500">{memoError}</p>}
      </div>
    </div>
  );
}
