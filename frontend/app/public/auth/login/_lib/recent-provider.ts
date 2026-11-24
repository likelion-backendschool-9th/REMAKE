// "최근 로그인" 말풍선: 마지막으로 로그인에 성공한 제공자. 기기(localStorage)에 두고 로그아웃해도 남긴다(정책 "최근 로그인").
// 이 화면은 값을 읽어 보여 주기만 한다. 값은 로그인 연동(API 1)을 붙일 때 로그인 성공 뒤에 저장한다.
// 개발 중 확인: 브라우저 콘솔에서 localStorage.setItem("remake.recentLoginProvider", "kakao") 후 새로 고침

export const SOCIAL_PROVIDERS = ["kakao", "naver", "google"] as const;
export type SocialProvider = (typeof SOCIAL_PROVIDERS)[number];

export const RECENT_PROVIDER_KEY = "remake.recentLoginProvider";

function isSocialProvider(value: unknown): value is SocialProvider {
  return typeof value === "string" && (SOCIAL_PROVIDERS as readonly string[]).includes(value);
}

/** useSyncExternalStore용. 저장소를 못 쓰거나 값이 이상하면 null(말풍선 없음) */
export function getRecentProvider(): SocialProvider | null {
  try {
    const value = localStorage.getItem(RECENT_PROVIDER_KEY);
    return isSocialProvider(value) ? value : null;
  } catch {
    return null;
  }
}

/** useSyncExternalStore용 구독. 다른 탭에서 바뀐 값도 받는다. */
export function subscribeRecentProvider(listener: () => void) {
  const handleStorage = (event: StorageEvent) => {
    if (event.key === RECENT_PROVIDER_KEY) listener();
  };
  window.addEventListener("storage", handleStorage);
  return () => window.removeEventListener("storage", handleStorage);
}
