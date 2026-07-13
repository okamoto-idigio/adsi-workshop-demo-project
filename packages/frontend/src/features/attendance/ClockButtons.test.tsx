import { render } from "@testing-library/react";
import { type Mock, beforeEach, describe, expect, it, vi } from "vitest";
import { ClockButtons } from "./ClockButtons";
import { useClockIn, useClockOut, useTodayStatus } from "./useAttendance";

vi.mock("./useAttendance", () => ({
  useTodayStatus: vi.fn(),
  useClockIn: vi.fn(),
  useClockOut: vi.fn(),
}));

vi.mock("@/components/ui/skeleton", () => ({
  Skeleton: ({ className }: { className: string }) => (
    <div data-testid="skeleton" className={className} />
  ),
}));

const mockMutate = vi.fn();

function setupMocks(status: string, records: unknown[] = []) {
  (useTodayStatus as Mock).mockReturnValue({
    data: { status, records },
    isLoading: false,
  });
  (useClockIn as Mock).mockReturnValue({
    mutate: mockMutate,
    isPending: false,
  });
  (useClockOut as Mock).mockReturnValue({
    mutate: mockMutate,
    isPending: false,
  });
}

function getClockButtons(container: HTMLElement) {
  const buttons = container.querySelectorAll("button");
  const clockInBtn = Array.from(buttons).find((b) => b.textContent?.includes("出勤"))!;
  const clockOutBtn = Array.from(buttons).find((b) => b.textContent?.includes("退勤"))!;
  return { clockInBtn, clockOutBtn };
}

describe("ClockButtons", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("未出勤のとき出勤ボタンが有効で退勤ボタンが無効", () => {
    setupMocks("NOT_CLOCKED_IN");
    const { container } = render(<ClockButtons />);
    const { clockInBtn, clockOutBtn } = getClockButtons(container);

    expect(clockInBtn).toBeEnabled();
    expect(clockOutBtn).toBeDisabled();
  });

  it("ATT-04: 出勤中のとき出勤ボタンが無効で退勤ボタンが有効", () => {
    setupMocks("CLOCKED_IN", [
      { clockIn: "2025-01-15T00:00:00Z", clockOut: null },
    ]);
    const { container } = render(<ClockButtons />);
    const { clockInBtn, clockOutBtn } = getClockButtons(container);

    expect(clockInBtn).toBeDisabled();
    expect(clockOutBtn).toBeEnabled();
  });

  it("ATT-06: 退勤済みのとき再出勤ボタンが有効", () => {
    setupMocks("CLOCKED_OUT", [
      { clockIn: "2025-01-15T00:00:00Z", clockOut: "2025-01-15T08:00:00Z" },
    ]);
    const { container } = render(<ClockButtons />);
    const { clockInBtn, clockOutBtn } = getClockButtons(container);

    expect(clockInBtn).toBeEnabled();
    expect(clockOutBtn).toBeDisabled();
  });
});
