"use client";

import { type ReactNode, useEffect, useState } from "react";

import { env } from "@/lib/env";

// NEXT_PUBLIC_API_MOCKING=true 일 때만 MSW를 켜고, 준비될 때까지 화면 렌더링을 미룬다
export function MswProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(!env.apiMocking);

  useEffect(() => {
    if (!env.apiMocking) return;
    import("@/mocks/browser")
      .then(({ worker }) => worker.start({ onUnhandledFrame: "bypass" }))
      .then(() => setReady(true));
  }, []);

  return ready ? children : null;
}
