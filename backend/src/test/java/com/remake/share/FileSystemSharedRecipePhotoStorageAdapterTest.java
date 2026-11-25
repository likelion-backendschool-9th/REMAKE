package com.remake.share;

import static com.remake.share.SharedRecipePhotoValidationTest.png;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.remake.global.config.UploadProperties;
import com.remake.global.error.BusinessException;
import com.remake.share.adapter.out.filestorage.FileSystemSharedRecipePhotoStorageAdapter;
import com.remake.share.application.port.out.PhotoStorageKeyConflictException;

class FileSystemSharedRecipePhotoStorageAdapterTest {

	@TempDir
	Path directory;

	private FileSystemSharedRecipePhotoStorageAdapter adapter;

	@BeforeEach
	void setUp() throws Exception {
		adapter = new FileSystemSharedRecipePhotoStorageAdapter(new UploadProperties(directory.toString()));
	}

	@Test
	void 실제_파일을_저장하고_새_어댑터로_다시_읽고_삭제한다() throws Exception {
		String key = key("png");
		adapter.store(key, png(), "image/png");
		var restarted = new FileSystemSharedRecipePhotoStorageAdapter(new UploadProperties(directory.toString()));
		try (var photo = restarted.load(key).orElseThrow()) {
			assertThat(photo.stream().readAllBytes()).containsExactly(png());
			assertThat(photo.contentLength()).isEqualTo(png().length);
			assertThat(photo.contentType()).isEqualTo("image/png");
		}
		restarted.delete(key);
		restarted.delete(key);
		assertThat(restarted.load(key)).isEmpty();
		assertThat(directory.resolve(key)).doesNotExist();
	}

	@Test
	void 기존_파일은_덮어쓰지_않는다() throws Exception {
		String key = key("jpg");
		adapter.store(key, new byte[] {1, 2}, "image/jpeg");
		assertThatThrownBy(() -> adapter.store(key, new byte[] {3}, "image/jpeg"))
				.isInstanceOf(PhotoStorageKeyConflictException.class);
		assertThat(Files.readAllBytes(directory.resolve(key))).containsExactly(1, 2);
	}

	@Test
	void 경로_탈출과_다른_기능의_키를_거절한다() {
		for (String key : new String[] {"../outside.png", "/tmp/outside.png", "shared-recipes/../outside.png",
				"receipts/" + UUID.randomUUID() + ".png", "shared-recipes/file.svg"}) {
			assertThatThrownBy(() -> adapter.store(key, png(), "image/png")).isInstanceOf(BusinessException.class);
			assertThatThrownBy(() -> adapter.load(key)).isInstanceOf(BusinessException.class);
			assertThatThrownBy(() -> adapter.delete(key)).isInstanceOf(BusinessException.class);
		}
	}

	@Test
	void 파일_심볼릭_링크는_읽지_않고_삭제해도_대상은_보존한다() throws Exception {
		Path outside = directory.resolve("outside.png");
		Files.write(outside, png());
		String key = key("png");
		Files.createSymbolicLink(directory.resolve(key), outside);
		assertThatThrownBy(() -> adapter.load(key)).isInstanceOf(BusinessException.class);
		adapter.delete(key);
		assertThat(outside).exists();
	}

	@Test
	void 폴더_심볼릭_링크를_통한_외부_접근을_거절한다() throws Exception {
		Files.delete(directory.resolve("shared-recipes"));
		Path other = Files.createDirectory(directory.resolve("other"));
		Files.createSymbolicLink(directory.resolve("shared-recipes"), other);
		assertThatThrownBy(() -> adapter.store(key("png"), png(), "image/png")).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> new FileSystemSharedRecipePhotoStorageAdapter(new UploadProperties(directory.toString())))
				.isInstanceOf(java.io.IOException.class);
	}

	@Test
	void 확장자에_맞는_MIME을_반환한다() throws Exception {
		for (String extension : new String[] {"jpg", "webp"}) {
			String key = key(extension);
			String type = "jpg".equals(extension) ? "image/jpeg" : "image/webp";
			adapter.store(key, new byte[] {1}, type);
			try (var content = adapter.load(key).orElseThrow()) {
				assertThat(content.contentType()).isEqualTo(type);
			}
		}
	}

	private String key(String extension) {
		return "shared-recipes/" + UUID.randomUUID() + "." + extension;
	}
}
