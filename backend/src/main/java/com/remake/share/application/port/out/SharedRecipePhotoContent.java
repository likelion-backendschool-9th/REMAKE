package com.remake.share.application.port.out;

import java.io.IOException;
import java.io.InputStream;

/** 사진 응답을 위한 스트림·길이·MIME. 실제 파일 경로는 외부로 전달하지 않는다. */
public record SharedRecipePhotoContent(InputStream stream, long contentLength, String contentType)
		implements AutoCloseable {

	@Override
	public void close() throws IOException {
		stream.close();
	}
}
