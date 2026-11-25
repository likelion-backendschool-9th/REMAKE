// 문서(body) 스크롤바를 OverlayScrollbars로 바꾼다.
// 기본 스크롤바는 자리(데스크톱 Windows 약 15px)를 차지해서, 시트·모달이 열리며 스크롤을 막을 때 스크롤바가 빠지면
// 가운데 정렬된 화면(최대 480px)이 그 폭만큼 좌우로 움직였다. 겹쳐 그리는 스크롤바는 자리를 차지하지 않아 움직이지 않는다.
// 스크롤바는 스크롤할 때만 보이고 잠시 뒤 사라진다. window.scrollTo 같은 문서 스크롤 API는 그대로 쓴다.

import { OverlayScrollbars } from "overlayscrollbars";

let instance: OverlayScrollbars | null = null;
let savedOverflow = "";

/** 앱이 처음 그려진 뒤 한 번 부른다(AppProviders). 돌려준 함수로 되돌린다 */
export function initPageScrollbar(): () => void {
  const created = OverlayScrollbars(
    { target: document.body },
    {
      scrollbars: {
        // 스크롤할 때만 보인다. 처음 스크롤하기 전에는 숨긴다
        autoHide: "scroll",
        autoHideSuspend: true,
      },
    },
  );
  instance = created;
  return () => {
    created.destroy();
    if (instance === created) instance = null;
  };
}

/**
 * 뒤 화면 스크롤을 막거나 푼다(useDialogLayer). OverlayScrollbars가 붙어 있으면 그 overflow 옵션으로 막아
 * 화면 폭이 그대로다. 붙지 않았으면(초기화 전·취소) html overflow로 막는다.
 */
export function setPageScrollLocked(locked: boolean) {
  if (instance && !instance.state().destroyed) {
    const overflow = locked ? "hidden" : "scroll";
    instance.options({ overflow: { x: overflow, y: overflow } });
    return;
  }
  const html = document.documentElement;
  if (locked) {
    savedOverflow = html.style.overflow;
    html.style.overflow = "hidden";
  } else {
    html.style.overflow = savedOverflow;
  }
}
