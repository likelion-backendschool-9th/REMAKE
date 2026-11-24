package com.remake.share.adapter.out.filestorage;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.remake.global.config.UploadProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.out.LoadSharedRecipePhotoPort;
import com.remake.share.application.port.out.PhotoStorageKeyConflictException;
import com.remake.share.application.port.out.SharedRecipePhotoContent;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;

/** blue/green이 공유하는 볼륨의 shared-recipes 하위 폴더만 사용한다. */
@Component
public class FileSystemSharedRecipePhotoStorageAdapter
		implements SharedRecipePhotoStoragePort, LoadSharedRecipePhotoPort {

	private static final Pattern KEY = Pattern.compile(
			"shared-recipes/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");
	private final Path photoDirectory;

	public FileSystemSharedRecipePhotoStorageAdapter(UploadProperties properties) throws IOException {
		Path root = Path.of(properties.rootDirectory()).toAbsolutePath().normalize();
		Files.createDirectories(root);
		photoDirectory = root.toRealPath().resolve("shared-recipes");
		Files.createDirectories(photoDirectory);
		if (Files.isSymbolicLink(photoDirectory)) {
			throw new IOException("공유 사진 폴더는 심볼릭 링크일 수 없습니다.");
		}
	}

	@Override
	public void store(String imageKey, byte[] content, String contentType) {
		Path target = resolve(imageKey);
		try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW,
				StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
			output.write(content);
		} catch (FileAlreadyExistsException failure) {
			throw new PhotoStorageKeyConflictException();
		} catch (IOException failure) {
			throw unavailable(failure);
		}
	}

	@Override
	public void delete(String imageKey) {
		Path target = resolve(imageKey);
		try {
			Files.deleteIfExists(target);
		} catch (IOException failure) {
			throw unavailable(failure);
		}
	}

	@Override
	public Optional<SharedRecipePhotoContent> load(String imageKey) {
		Path target = resolve(imageKey);
		try {
			SeekableByteChannel channel = Files.newByteChannel(target,
					Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
			try {
				String contentType = switch (imageKey.substring(imageKey.lastIndexOf('.') + 1)) {
					case "jpg" -> "image/jpeg";
					case "png" -> "image/png";
					default -> "image/webp";
				};
				return Optional.of(new SharedRecipePhotoContent(Channels.newInputStream(channel),
						channel.size(), contentType));
			} catch (IOException | RuntimeException failure) {
				try {
					channel.close();
				} catch (IOException closeFailure) {
					failure.addSuppressed(closeFailure);
				}
				throw failure;
			}
		} catch (NoSuchFileException failure) {
			return Optional.empty();
		} catch (IOException failure) {
			throw unavailable(failure);
		}
	}

	private Path resolve(String imageKey) {
		if (imageKey == null || !KEY.matcher(imageKey).matches()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		// 키를 제한하고 상위 폴더가 나중에 링크로 바뀐 경우도 거절한다.
		if (Files.isSymbolicLink(photoDirectory)) {
			throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
		}
		return photoDirectory.resolve(imageKey.substring("shared-recipes/".length()));
	}

	private BusinessException unavailable(IOException cause) {
		BusinessException error = new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
		error.initCause(cause);
		return error;
	}
}
