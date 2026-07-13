import { apiClient } from "@/lib/api-client";

export type LeaveType = "FULL_DAY" | "AM_HALF" | "PM_HALF";
export type LeaveRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "WITHDRAWN";

export interface PaidLeaveResponse {
  id: string;
  requesterId: string;
  requesterName: string;
  approverId: string | null;
  approverName: string | null;
  startDate: string;
  endDate: string;
  leaveType: LeaveType;
  leaveDays: number;
  status: LeaveRequestStatus;
  rejectReason: string | null;
  version: number;
  createdAt: string;
}

export interface PendingLeaveResponse {
  id: string;
  requesterId: string;
  requesterName: string;
  startDate: string;
  endDate: string;
  leaveType: LeaveType;
  leaveDays: number;
  status: LeaveRequestStatus;
  version: number;
  createdAt: string;
}

export interface PaidLeaveCreateRequest {
  startDate: string;
  endDate: string;
  leaveType: LeaveType;
}

export function createPaidLeave(
  requesterId: string,
  request: PaidLeaveCreateRequest,
): Promise<PaidLeaveResponse> {
  return apiClient.post<PaidLeaveResponse>(`/api/paid-leaves?requesterId=${requesterId}`, request);
}

export function fetchPaidLeaves(
  requesterId: string,
  status?: LeaveRequestStatus,
): Promise<PaidLeaveResponse[]> {
  const params = new URLSearchParams({ requesterId });
  if (status) {
    params.set("status", status);
  }
  return apiClient.get<PaidLeaveResponse[]>(`/api/paid-leaves?${params.toString()}`);
}

export function withdrawPaidLeave(
  id: string,
  requesterId: string,
  version: number,
): Promise<PaidLeaveResponse> {
  return apiClient.patch<PaidLeaveResponse>(
    `/api/paid-leaves/${id}/withdraw?requesterId=${requesterId}`,
    { version },
  );
}

export function fetchPendingLeaves(managerId: string): Promise<PendingLeaveResponse[]> {
  return apiClient.get<PendingLeaveResponse[]>(`/api/paid-leaves/pending?managerId=${managerId}`);
}

export function approvePaidLeave(
  id: string,
  approverId: string,
  version: number,
): Promise<PaidLeaveResponse> {
  return apiClient.patch<PaidLeaveResponse>(
    `/api/paid-leaves/${id}/approve?approverId=${approverId}&version=${version}`,
  );
}

export function rejectPaidLeave(
  id: string,
  approverId: string,
  reason: string,
  version: number,
): Promise<PaidLeaveResponse> {
  return apiClient.patch<PaidLeaveResponse>(
    `/api/paid-leaves/${id}/reject?approverId=${approverId}`,
    { reason, version },
  );
}
