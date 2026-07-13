"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/components/Toast";
import { useAuth } from "@/features/auth/useAuth";
import {
  approvePaidLeave,
  createPaidLeave,
  fetchPaidLeaves,
  fetchPendingLeaves,
  type LeaveRequestStatus,
  type PaidLeaveCreateRequest,
  rejectPaidLeave,
  withdrawPaidLeave,
} from "./paid-leave-api";

const PAID_LEAVES_KEY = ["paid-leaves"] as const;
const PENDING_LEAVES_KEY = ["paid-leaves", "pending"] as const;

export function usePaidLeaves(status?: LeaveRequestStatus) {
  const { user } = useAuth();
  const requesterId = user?.id;

  return useQuery({
    queryKey: [...PAID_LEAVES_KEY, requesterId, status],
    queryFn: () => fetchPaidLeaves(requesterId!, status),
    enabled: !!requesterId,
  });
}

export function useCreatePaidLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: PaidLeaveCreateRequest) => createPaidLeave(user!.id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PAID_LEAVES_KEY });
      toast.success("有給申請を送信しました");
    },
    onError: () => {
      toast.error("有給申請の送信に失敗しました");
    },
  });
}

export function useWithdrawPaidLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      withdrawPaidLeave(id, user!.id, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PAID_LEAVES_KEY });
      toast.success("有給申請を取り下げました");
    },
    onError: () => {
      toast.error("取り下げに失敗しました");
    },
  });
}

export function usePendingLeaves() {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...PENDING_LEAVES_KEY, user?.id],
    queryFn: () => fetchPendingLeaves(user!.id),
    enabled: !!user?.isManager,
  });
}

export function useApprovePaidLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      approvePaidLeave(id, user!.id, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PENDING_LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: PAID_LEAVES_KEY });
      toast.success("有給申請を承認しました");
    },
    onError: () => {
      toast.error("承認に失敗しました");
    },
  });
}

export function useRejectPaidLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, reason, version }: { id: string; reason: string; version: number }) =>
      rejectPaidLeave(id, user!.id, reason, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PENDING_LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: PAID_LEAVES_KEY });
      toast.success("有給申請を却下しました");
    },
    onError: () => {
      toast.error("却下に失敗しました");
    },
  });
}
