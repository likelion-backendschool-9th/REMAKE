package com.remake.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

/** 통합 테스트의 DB 정리. 기준 데이터 테이블은 남기고 나머지 테이블은 테스트가 끝날 때마다 비운다. */
class TestDatabase {

	private final JdbcTemplate jdbc;
	private final Set<String> referenceTables;
	private List<String> tablesToClean;
	private Map<String, String> referenceSnapshot;

	TestDatabase(JdbcTemplate jdbc, Set<String> referenceTables) {
		this.jdbc = jdbc;
		this.referenceTables = referenceTables;
	}

	/** 마이그레이션과 앱 시작 코드가 모두 끝난 뒤, 테스트 코드가 돌기 전에 한 번 실행된다. */
	@EventListener(ApplicationReadyEvent.class)
	void inspect() {
		List<String> tables = jdbc.queryForList(
				"SELECT tablename FROM pg_tables WHERE schemaname = current_schema() AND tablename <> 'flyway_schema_history'",
				String.class);
		tablesToClean = tables.stream().filter(t -> !referenceTables.contains(t)).toList();
		List<String> notEmpty = tablesToClean.stream().filter(this::hasRows).toList();
		if (!notEmpty.isEmpty()) {
			throw new IllegalStateException("앱이 뜬 직후 행이 있는 테이블: " + notEmpty
					+ ". 기준 데이터면 IntegrationTestSupport.REFERENCE_TABLES에 추가하세요."
					+ " 사용자 데이터 테이블이면 마이그레이션이나 앱 시작 코드로 고정 행을 넣지 마세요."
					+ " (docs/developer/backend-testing.md)");
		}
		referenceSnapshot = snapshot(tables.stream().filter(referenceTables::contains).toList());
	}

	void cleanUp() {
		if (tablesToClean == null) {
			throw new IllegalStateException("TestDatabase.inspect가 실행되지 않았습니다.");
		}
		Map<String, String> now = snapshot(List.copyOf(referenceSnapshot.keySet()));
		List<String> changed = now.keySet().stream().filter(t -> !now.get(t).equals(referenceSnapshot.get(t))).toList();
		if (!tablesToClean.isEmpty()) {
			// CASCADE를 붙이지 않는다. "cannot truncate a table referenced in a foreign key constraint"가 나면
			// 기준 데이터 테이블이 사용자 데이터를 참조하는 설계 문제다. CASCADE를 붙이면 기준 데이터가 조용히 지워진다.
			String names = String.join(", ", tablesToClean.stream().map(t -> "\"" + t + "\"").toList());
			jdbc.execute("TRUNCATE TABLE " + names + " RESTART IDENTITY");
		}
		if (!changed.isEmpty()) {
			referenceSnapshot = now; // 다음 테스트가 같은 이유로 실패하지 않게 다시 기록한다
			throw new AssertionError("이 테스트가 기준 데이터를 바꿨습니다: " + changed + ". 기준 데이터는 마이그레이션으로만 바꿉니다."
					+ " (docs/developer/backend-testing.md)");
		}
	}

	private boolean hasRows(String table) {
		return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM \"" + table + "\")", Boolean.class));
	}

	// 테이블 내용의 요약값. 행을 넣거나 바꾸거나 지우면 달라진다.
	private Map<String, String> snapshot(List<String> tables) {
		Map<String, String> result = new LinkedHashMap<>();
		for (String t : tables) {
			result.put(t, jdbc.queryForObject(
					"SELECT md5(coalesce(string_agg(r::text, ',' ORDER BY r::text), '')) FROM \"" + t + "\" r", String.class));
		}
		return result;
	}
}
