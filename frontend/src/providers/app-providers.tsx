"use client";

import type { ReactNode } from "react";

import { SerwistProvider } from "@serwist/turbopack/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

import { env } from "@/lib/env";
import { getQueryClient } from "@/lib/query-client";

import { MswProvider } from "./msw-provider";

export function AppProviders({ children }: { children: ReactNode }) {
  const queryClient = getQueryClient();

  return (
    // 개발 중이거나 MSW 사용 중에는 Serwist 서비스 워커를 끈다 (같은 scope 충돌 방지)
    <SerwistProvider
      swUrl="/serwist/sw.js"
      disable={process.env.NODE_ENV === "development" || env.apiMocking}
    >
      <QueryClientProvider client={queryClient}>
        <MswProvider>{children}</MswProvider>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </SerwistProvider>
  );
}
