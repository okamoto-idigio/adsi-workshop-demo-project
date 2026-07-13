import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLogout } from "./useAuth";

const mockPush = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

vi.mock("./auth-api", () => ({
  logout: vi.fn(),
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
}));

import { logout } from "./auth-api";

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return {
    queryClient,
    Wrapper: ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
  };
}

describe("useLogout", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("ログアウトAPI成功後にログイン画面へ遷移する", async () => {
    vi.mocked(logout).mockResolvedValue(undefined);
    const { queryClient, Wrapper } = createWrapper();
    queryClient.setQueryData(["auth", "me"], { id: "1", name: "Test" });

    const { result } = renderHook(() => useLogout(), { wrapper: Wrapper });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/login");
    });
    expect(queryClient.getQueryData(["auth", "me"])).toBeUndefined();
  });

  it("ログアウトAPIが失敗してもログイン画面へ遷移する", async () => {
    vi.mocked(logout).mockRejectedValue(new Error("Network error"));
    const { queryClient, Wrapper } = createWrapper();
    queryClient.setQueryData(["auth", "me"], { id: "1", name: "Test" });

    const { result } = renderHook(() => useLogout(), { wrapper: Wrapper });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/login");
    });
    expect(queryClient.getQueryData(["auth", "me"])).toBeUndefined();
  });
});
