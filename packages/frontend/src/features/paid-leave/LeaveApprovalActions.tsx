"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useApprovePaidLeave, useRejectPaidLeave } from "./usePaidLeaves";

interface LeaveApprovalActionsProps { leaveId: string; version: number; }

export function LeaveApprovalActions({ leaveId, version }: LeaveApprovalActionsProps) {
  const [showRejectDialog, setShowRejectDialog] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const approveMutation = useApprovePaidLeave();
  const rejectMutation = useRejectPaidLeave();

  function handleReject() {
    if (!rejectReason.trim()) return;
    rejectMutation.mutate({ id: leaveId, reason: rejectReason, version }, { onSuccess: () => { setShowRejectDialog(false); setRejectReason(""); } });
  }

  return (
    <>
      <div className="flex gap-2">
        <Button size="sm" onClick={() => approveMutation.mutate({ id: leaveId, version })} disabled={approveMutation.isPending}>承認</Button>
        <Button size="sm" variant="destructive" onClick={() => setShowRejectDialog(true)} disabled={rejectMutation.isPending}>却下</Button>
      </div>
      <Dialog open={showRejectDialog} onOpenChange={setShowRejectDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>却下理由</DialogTitle></DialogHeader>
          <Input value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} placeholder="却下理由を入力してください" maxLength={500} />
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowRejectDialog(false)}>キャンセル</Button>
            <Button variant="destructive" onClick={handleReject} disabled={!rejectReason.trim() || rejectMutation.isPending}>却下する</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
