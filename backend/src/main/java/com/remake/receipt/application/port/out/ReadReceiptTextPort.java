package com.remake.receipt.application.port.out;

import java.time.Duration;

public interface ReadReceiptTextPort {

	String read(byte[] image, Duration timeout);
}
