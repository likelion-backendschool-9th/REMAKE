package com.remake.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.remake.event.domain.EventName;

// docs/agent/spec/events.md 의 이벤트 표(79개)와 EventName enum이 이름·발송 주체·허용 속성까지 같은지 확인한다(#196).
// 문서만 고치거나 코드만 고쳐서 어긋나면, 수집 API(56)가 이름을 몰라 조용히 거부하는 일이 생기므로 여기서 먼저 잡는다.
class EventContractTest {

	private static final Pattern ROW = Pattern
			.compile("^\\| (E\\d+) \\| `(\\w+)` \\| (CLIENT|SERVER) \\| [^|]+ \\| ([^|]*) \\|");

	private record DocEvent(String id, String source, Set<String> properties) {
	}

	private Map<String, DocEvent> readDocEvents() throws IOException {
		Path doc = Path.of("..", "docs", "agent", "spec", "events.md");
		Map<String, DocEvent> events = new LinkedHashMap<>();
		for (String line : Files.readAllLines(doc)) {
			Matcher m = ROW.matcher(line);
			if (!m.find()) {
				continue;
			}
			String raw = m.group(4).trim();
			Set<String> properties = raw.startsWith("(common only)") ? Set.of()
					: Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty())
							.collect(Collectors.toSet());
			events.put(m.group(2), new DocEvent(m.group(1), m.group(3), properties));
		}
		return events;
	}

	@Test
	void 문서의_79개_이벤트와_EventName이름이_같다() throws IOException {
		Map<String, DocEvent> doc = readDocEvents();

		Set<String> code = Arrays.stream(EventName.values()).map(EventName::getWireName).collect(Collectors.toSet());

		assertThat(doc).hasSize(79);
		assertThat(code).containsExactlyInAnyOrderElementsOf(doc.keySet());
	}

	@Test
	void 발송_주체와_허용_속성이_문서와_같다() throws IOException {
		Map<String, DocEvent> doc = readDocEvents();
		Set<String> problems = new HashSet<>();

		for (EventName name : EventName.values()) {
			DocEvent expected = doc.get(name.getWireName());
			if (expected == null) {
				continue; // 이름 불일치는 위 테스트가 잡는다
			}
			if (!name.getSource().name().equals(expected.source())) {
				problems.add(name.getWireName() + " source: 코드 " + name.getSource() + " / 문서 " + expected.source());
			}
			if (!name.getAllowedProperties().equals(expected.properties())) {
				problems.add(name.getWireName() + " properties: 코드 " + name.getAllowedProperties() + " / 문서 "
						+ expected.properties());
			}
		}

		assertThat(problems).isEmpty();
	}

	@Test
	void 문서의_CLIENT_45개_SERVER_34개_구성이_맞다() throws IOException {
		Map<String, DocEvent> doc = readDocEvents();

		long client = doc.values().stream().filter(e -> e.source().equals("CLIENT")).count();
		long server = doc.values().stream().filter(e -> e.source().equals("SERVER")).count();

		assertThat(client).isEqualTo(45);
		assertThat(server).isEqualTo(34);
	}
}
