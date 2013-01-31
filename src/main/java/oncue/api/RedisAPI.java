package oncue.api;

import java.util.Map;

import oncue.backingstore.RedisBackingStore;
import oncue.messages.internal.Job;

/**
 * An implementation of the {@linkplain API} that uses Redis as an intermediary.
 */
public class RedisAPI implements API {

	// The singleton instance
	private static API instance;

	/**
	 * Get the API singleton instance
	 * 
	 * @return an API instance that implements the {@linkplain API} interface
	 */
	public static API getInstance() {
		if (instance == null) {
			instance = new RedisAPI();
		}
		return instance;
	}

	@Override
	public Job enqueueJob(String workerType) throws APIException {
		return RedisBackingStore.createJob(workerType, null);
	}

	@Override
	public Job enqueueJob(String workerType, Map<String, String> jobParams) throws APIException {
		return RedisBackingStore.createJob(workerType, jobParams);
	}

}
