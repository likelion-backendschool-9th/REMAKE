package com.remake.share.application.port.out;

import java.util.List;

public interface SharedRecipePhotoDeletionPort {

	void enqueue(String imageKey);

	boolean lock(String imageKey);

	List<String> lockPending();

	void complete(String imageKey);

	void defer(String imageKey);
}
