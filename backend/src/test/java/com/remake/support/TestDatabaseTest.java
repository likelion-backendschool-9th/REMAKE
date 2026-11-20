package com.remake.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

// TestDatabase의 검사가 동작하는지 확인한다. 모든 통합 테스트가 이 검사를 전제로 하므로, 검사가 빠지거나 바뀌면 여기서 실패한다.
// 공유 앱의 TestDatabase는 건드리지 않고, 임시 테이블(tdb_)과 그 테이블을 아는 TestDatabase를 따로 만들어 쓴다.
class TestDatabaseTest extends IntegrationTestSupport {

	private TestDatabase database;

	@BeforeEach
	void createTables() {
		jdbcTemplate.execute("CREATE TABLE tdb_reference (code VARCHAR(10) PRIMARY KEY)");
		jdbcTemplate.execute("INSERT INTO tdb_reference VALUES ('A')");
		jdbcTemplate.execute("CREATE TABLE tdb_user_data (id BIGSERIAL PRIMARY KEY)");
		database = new TestDatabase(jdbcTemplate, referenceTablesWith("tdb_reference"));
	}

	// 부모의 정리보다 먼저 실행된다. 공유 앱의 TestDatabase는 임시 테이블을 모르므로 여기서 지운다.
	@AfterEach
	void dropTables() {
		jdbcTemplate.execute("DROP TABLE IF EXISTS tdb_reference_with_fk, tdb_reference, tdb_user_data");
	}

	// 실제 기준 데이터 테이블도 넣어야 기준 데이터 마이그레이션이 생긴 뒤에도 inspect가 그 테이블의 행 때문에 실패하지 않는다.
	private static Set<String> referenceTablesWith(String... tables) {
		Set<String> result = new HashSet<>(REFERENCE_TABLES);
		result.addAll(Set.of(tables));
		return result;
	}

	private Long count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
	}

	@Test
	void 기준_데이터를_바꾸면_실패하고_다음_정리는_통과한다() {
		database.inspect();
		jdbcTemplate.update("INSERT INTO tdb_reference VALUES ('B')");

		assertThatThrownBy(database::cleanUp).isInstanceOf(AssertionError.class).hasMessageContaining("tdb_reference");
		assertThatCode(database::cleanUp).doesNotThrowAnyException();
	}

	@Test
	void 사용자_데이터는_비우고_기준_데이터는_남긴다() {
		database.inspect();
		jdbcTemplate.update("INSERT INTO tdb_user_data DEFAULT VALUES");

		database.cleanUp();

		assertThat(count("tdb_user_data")).isZero();
		assertThat(count("tdb_reference")).isEqualTo(1);
	}

	@Test
	void 앱이_뜬_직후_사용자_데이터_테이블에_행이_있으면_실패한다() {
		jdbcTemplate.update("INSERT INTO tdb_user_data DEFAULT VALUES");

		assertThatThrownBy(database::inspect).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("tdb_user_data");
	}

	// 비우기에 CASCADE를 붙이면 이 테스트가 실패한다. 기준 데이터가 조용히 지워지는 대신 비우기가 실패해야 한다.
	@Test
	void 기준_데이터_테이블이_사용자_데이터를_참조하면_비우기가_실패한다() {
		jdbcTemplate.execute("CREATE TABLE tdb_reference_with_fk (code VARCHAR(10) PRIMARY KEY, "
				+ "user_data_id BIGINT REFERENCES tdb_user_data (id))");
		jdbcTemplate.execute("INSERT INTO tdb_reference_with_fk VALUES ('A', NULL)");
		TestDatabase withFk = new TestDatabase(jdbcTemplate, referenceTablesWith("tdb_reference", "tdb_reference_with_fk"));
		withFk.inspect();

		assertThatThrownBy(withFk::cleanUp).isInstanceOf(DataAccessException.class)
				.hasMessageContaining("cannot truncate a table referenced in a foreign key constraint");
		assertThat(count("tdb_reference_with_fk")).isEqualTo(1);
	}
}
