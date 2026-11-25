"use client";

import { type ReactNode, useEffect } from "react";

import { SerwistProvider } from "@serwist/turbopack/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { NuqsAdapter } from "nuqs/adapters/next/app";

import { env } from "@/lib/env";
import { initPageScrollbar } from "@/lib/page-scrollbar";
import { getQueryClient } from "@/lib/query-client";

import { MswProvider } from "./msw-provider";

export function AppProviders({ children }: { children: ReactNode }) {
  const queryClient = getQueryClient();

  // 문서 스크롤바를 겹쳐 그리는 스크롤바로 바꾼다(시트가 열려도 화면이 좌우로 움직이지 않게, lib/page-scrollbar)
  useEffect(() => initPageScrollbar(), []);

  return (
    // 개발 중이거나 MSW 사용 중에는 Serwist 서비스 워커를 끈다 (같은 scope 충돌 방지)
    <SerwistProvider
      swUrl="/serwist/sw.js"
      disable={process.env.NODE_ENV === "development" || env.apiMocking}
    >
      <QueryClientProvider client={queryClient}>
        <NuqsAdapter>
          <MswProvider>{children}</MswProvider>
        </NuqsAdapter>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </SerwistProvider>
  );
}
