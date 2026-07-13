"use client";

import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { LeaveApprovalActions } from "./LeaveApprovalActions";
import { usePendingLeaves } from "./usePaidLeaves";

const LEAVE_TYPE_LABELS: Record<string, string> = { FULL_DAY: "全休", AM_HALF: "午前休", PM_HALF: "午後休" };

export function PendingLeaveList() {
  const { data: leaves, isLoading } = usePendingLeaves();
  if (isLoading) return <p className="text-muted-foreground">読み込み中...</p>;

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold">承認待ち有給申請</h3>
      <Table>
        <TableHeader><TableRow><TableHead>申請者</TableHead><TableHead>開始日</TableHead><TableHead>終了日</TableHead><TableHead>種別</TableHead><TableHead>日数</TableHead><TableHead>申請日</TableHead><TableHead></TableHead></TableRow></TableHeader>
        <TableBody>
          {leaves && leaves.length > 0 ? leaves.map((leave) => (
            <TableRow key={leave.id}>
              <TableCell>{leave.requesterName}</TableCell>
              <TableCell>{leave.startDate}</TableCell>
              <TableCell>{leave.endDate}</TableCell>
              <TableCell>{LEAVE_TYPE_LABELS[leave.leaveType]}</TableCell>
              <TableCell>{leave.leaveDays}</TableCell>
              <TableCell>{new Date(leave.createdAt).toLocaleDateString("ja-JP")}</TableCell>
              <TableCell><LeaveApprovalActions leaveId={leave.id} version={leave.version} /></TableCell>
            </TableRow>
          )) : <TableRow><TableCell colSpan={7} className="text-center text-muted-foreground">承認待ちの申請はありません</TableCell></TableRow>}
        </TableBody>
      </Table>
    </div>
  );
}
