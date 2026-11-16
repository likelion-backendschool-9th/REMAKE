/// <reference lib="esnext" />
/// <reference lib="webworker" />
import { defaultCache } from "@serwist/turbopack/worker";
import type { PrecacheEntry, SerwistGlobalConfig } from "serwist";
import { Serwist } from "serwist";

declare global {
  interface WorkerGlobalScope extends SerwistGlobalConfig {
    __SW_MANIFEST: (PrecacheEntry | string)[] | undefined;
  }
}

declare const self: ServiceWorkerGlobalScope;

const serwist = new Serwist({
  precacheEntries: self.__SW_MANIFEST,
  skipWaiting: true,
  clientsClaim: true,
  navigationPreload: true,
  runtimeCaching: defaultCache,
  fallbacks: {
    entries: [
      {
        url: "/~offline",
        matcher: ({ request }) => request.destination === "document",
      },
    ],
  },
});

type PushPayload = { title?: string; body?: string; url?: string };

// Web Push: 소비기한·재구매 알림 수신
self.addEventListener("push", (event) => {
  const data = event.data?.json() as PushPayload | undefined;
  event.waitUntil(
    self.registration.showNotification(data?.title ?? "REMAKE", {
      body: data?.body,
      icon: "/favicon.ico",
      data: { url: data?.url ?? "/" },
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = (event.notification.data as PushPayload | undefined)?.url ?? "/";
  event.waitUntil(self.clients.openWindow(url));
});

serwist.addEventListeners();
