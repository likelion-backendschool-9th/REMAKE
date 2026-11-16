import createClient from "openapi-fetch";

import { env } from "@/lib/env";

import type { paths } from "./schema";

export const api = createClient<paths>({
  baseUrl: env.apiBaseUrl,
  credentials: "include",
});
