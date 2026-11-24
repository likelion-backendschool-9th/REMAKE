package com.remake.receipt.adapter.out.filestorage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalReceiptImageStorageTest {

	@TempDir
	Path directory;

	@Test
	void 사진을_저장하고_같은_키로_삭제할_수_있다() throws IOException {
		var storage = new LocalReceiptImageStorage(directory);
		byte[] image = {1, 2, 3};

		String imageKey = storage.save(new ByteArrayInputStream(image));
		assertArrayEquals(image, Files.readAllBytes(directory.resolve(imageKey)));

		storage.delete(imageKey);
		storage.delete(imageKey);
		assertFalse(Files.exists(directory.resolve(imageKey)));
		assertThrows(IllegalArgumentException.class, () -> storage.delete("../outside"));
	}

	@Test
	void 사진을_읽지_못하면_임시_파일을_정리한다() throws IOException {
		var storage = new LocalReceiptImageStorage(directory);
		var brokenImage = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("Cannot read image");
			}
		};

		assertThrows(IOException.class, () -> storage.save(brokenImage));
		try (var files = Files.list(directory)) {
			assertFalse(files.findAny().isPresent());
		}
	}
}
