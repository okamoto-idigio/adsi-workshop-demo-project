"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import type { LeaveRequestStatus } from "./paid-leave-api";
import { usePaidLeaves, useWithdrawPaidLeave } from "./usePaidLeaves";

const LEAVE_TYPE_LABELS: Record<string, string> = { FULL_DAY: "全休", AM_HALF: "午前休", PM_HALF: "午後休" };
const STATUS_LABELS: Record<string, string> = { PENDING: "申請中", APPROVED: "承認済み", REJECTED: "却下", WITHDRAWN: "取り下げ済み" };
const STATUS_COLORS: Record<string, string> = { PENDING: "text-yellow-600", APPROVED: "text-green-600", REJECTED: "text-red-600", WITHDRAWN: "text-gray-500" };

export function PaidLeaveList() {
  const [statusFilter, setStatusFilter] = useState<LeaveRequestStatus | "ALL">("ALL");
  const { data: leaves, isLoading } = usePaidLeaves(statusFilter === "ALL" ? undefined : statusFilter);
  const withdrawMutation = useWithdrawPaidLeave();

  if (isLoading) return <p className="text-muted-foreground">読み込み中...</p>;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">申請一覧</h3>
        <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v as LeaveRequestStatus | "ALL")}>
          <SelectTrigger className="w-36"><SelectValue>{(value: string | null) => {
            const labels: Record<string, string> = { ALL: "すべて", PENDING: "申請中", APPROVED: "承認済み", REJECTED: "却下", WITHDRAWN: "取り下げ済み" };
            return labels[value ?? ""] ?? "すべて";
          }}</SelectValue></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">すべて</SelectItem>
            <SelectItem value="PENDING">申請中</SelectItem>
            <SelectItem value="APPROVED">承認済み</SelectItem>
            <SelectItem value="REJECTED">却下</SelectItem>
            <SelectItem value="WITHDRAWN">取り下げ済み</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <Table>
        <TableHeader><TableRow><TableHead>開始日</TableHead><TableHead>終了日</TableHead><TableHead>種別</TableHead><TableHead>日数</TableHead><TableHead>ステータス</TableHead><TableHead>承認者</TableHead><TableHead></TableHead></TableRow></TableHeader>
        <TableBody>
          {leaves && leaves.length > 0 ? leaves.map((leave) => (
            <TableRow key={leave.id}>
              <TableCell>{leave.startDate}</TableCell>
              <TableCell>{leave.endDate}</TableCell>
              <TableCell>{LEAVE_TYPE_LABELS[leave.leaveType]}</TableCell>
              <TableCell>{leave.leaveDays}</TableCell>
              <TableCell className={STATUS_COLORS[leave.status]}>{STATUS_LABELS[leave.status]}</TableCell>
              <TableCell>{leave.approverName ?? "—"}</TableCell>
              <TableCell>{leave.status === "PENDING" && <Button variant="ghost" size="sm" onClick={() => withdrawMutation.mutate({ id: leave.id, version: leave.version })} disabled={withdrawMutation.isPending}>取り下げ</Button>}</TableCell>
            </TableRow>
          )) : <TableRow><TableCell colSpan={7} className="text-center text-muted-foreground">申請がありません</TableCell></TableRow>}
        </TableBody>
      </Table>
    </div>
  );
}
