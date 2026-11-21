package com.remake.auth.application.port.out;

public interface TokenHasherPort {

	String hash(String rawToken);
}
