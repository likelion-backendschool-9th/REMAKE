package com.remake.receipt.adapter.out.filestorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.remake.global.config.UploadProperties;
import com.remake.receipt.application.port.out.ReceiptImageStoragePort;

@Component
public class LocalReceiptImageStorage implements ReceiptImageStoragePort {

	private static final Pattern IMAGE_KEY = Pattern.compile("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}");
	private final Path directory;

	@Autowired
	public LocalReceiptImageStorage(UploadProperties properties) {
		this(Path.of(properties.rootDirectory(), "receipts"));
	}

	LocalReceiptImageStorage(Path directory) {
		this.directory = directory.toAbsolutePath().normalize();
	}

	@Override
	public String save(InputStream image) throws IOException {
		Files.createDirectories(directory);
		String imageKey = UUID.randomUUID().toString();
		// 사진을 전부 쓴 뒤 최종 경로로 옮겨, 저장 도중 끊긴 파일이 남지 않게 한다.
		Path temporaryFile = Files.createTempFile(directory, ".upload-", ".tmp");
		try {
			try (var output = Files.newOutputStream(temporaryFile)) {
				image.transferTo(output);
			}
			Files.move(temporaryFile, directory.resolve(imageKey));
			return imageKey;
		} catch (IOException | RuntimeException failure) {
			try {
				Files.deleteIfExists(temporaryFile);
			} catch (IOException cleanupFailure) {
				failure.addSuppressed(cleanupFailure);
			}
			throw failure;
		}
	}

	@Override
	public void delete(String imageKey) throws IOException {
		// 서버가 발급한 키만 허용해 사진 폴더 밖의 파일을 삭제하지 않게 한다.
		if (!IMAGE_KEY.matcher(imageKey).matches()) {
			throw new IllegalArgumentException("Invalid receipt image key");
		}
		Files.deleteIfExists(directory.resolve(imageKey));
	}
}
