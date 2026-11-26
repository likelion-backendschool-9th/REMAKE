import {
  debounce,
  parseAsArrayOf,
  parseAsBoolean,
  parseAsString,
  parseAsStringLiteral,
  useQueryStates,
} from "nuqs";

import { EVENT_SOURCES, type EventSource } from "../_lib/event-log";

/** 입력하는 동안 주소가 글자마다 바뀌지 않게, 화면 값은 바로 바꾸고 주소는 잠시 멈췄을 때 바꾼다 */
const TYPING_URL_UPDATE = { limitUrlUpdates: debounce(300) };

/**
 * a6 이벤트 로그의 검색 조건을 주소에 둔다(blueprint §5, nuqs). 키는 API 64 조회 조건과 같다:
 * ?name=&userId=&source=CLIENT&excludeSeed=false. 기본값이면 주소에서 뺀다. 고른 줄(상세)은 화면 상태로 둔다.
 * 이 훅을 쓰는 화면은 주소 값을 읽으므로 page에서 Suspense로 감싼다(cacheComponents)
 */
export function useEventLogQuery(defaultEventName = "") {
  const [query, setQuery] = useQueryStates({
    name: parseAsString.withDefault(defaultEventName),
    userId: parseAsString.withDefault(""),
    source: parseAsArrayOf(parseAsStringLiteral(EVENT_SOURCES)).withDefault([...EVENT_SOURCES]),
    excludeSeed: parseAsBoolean.withDefault(true),
  });

  return {
    eventName: query.name,
    userId: query.userId,
    sources: query.source,
    excludeSeed: query.excludeSeed,
    setEventName: (name: string) => void setQuery({ name }, TYPING_URL_UPDATE),
    setUserId: (userId: string) => void setQuery({ userId }, TYPING_URL_UPDATE),
    toggleSource: (source: EventSource) =>
      void setQuery((current) => ({
        source: current.source.includes(source)
          ? current.source.filter((item) => item !== source)
          : [...current.source, source],
      })),
    setExcludeSeed: (excludeSeed: boolean) => void setQuery({ excludeSeed }),
  };
}
