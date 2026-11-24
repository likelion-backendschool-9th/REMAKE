package com.remake.receipt.application.port.out;

import java.io.IOException;
import java.io.InputStream;

public interface ReceiptImageStoragePort {

	String save(InputStream image) throws IOException;

	void delete(String imageKey) throws IOException;
}
