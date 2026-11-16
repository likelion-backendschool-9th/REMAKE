// 서버(SSR) 번들용 빈 모듈. msw/browser는 Node 환경에서 불러올 수 없어서
// next.config.ts의 resolveAlias로 서버 쪽 import를 이 파일로 대체한다.
export const worker = {
  start: async () => undefined,
};
