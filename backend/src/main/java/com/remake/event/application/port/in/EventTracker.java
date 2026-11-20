package com.remake.event.application.port.in;

import java.util.Map;

import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;

/**
 * 서버 이벤트 발행 API. 다른 기능은 이 UseCase만 호출한다(문자열 이벤트 이름 금지).
 * <pre>
 * eventTracker.track(EventName.RECEIPT_SAVED, Screen.RECEIPT_RESULT, Map.of(
 *     "registered_count", registeredCount,
 *     "ocr_item_count", ocrItemCount),
 *     EventTrackCommand.ofUser(userId));
 * </pre>
 * 트랜잭션 커밋 뒤 비동기로 적재하고, 실패해도 호출한 API를 깨뜨리지 않는다.
 */
public interface EventTracker {

	void track(EventName name, Screen screen, Map<String, Object> properties, EventTrackCommand command);
}
