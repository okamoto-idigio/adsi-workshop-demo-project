"use client";

import { PaidLeaveForm } from "@/features/paid-leave/PaidLeaveForm";
import { PaidLeaveList } from "@/features/paid-leave/PaidLeaveList";

export default function PaidLeavesPage() {
  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold">有給休暇</h1>
      <PaidLeaveForm />
      <PaidLeaveList />
    </div>
  );
}
