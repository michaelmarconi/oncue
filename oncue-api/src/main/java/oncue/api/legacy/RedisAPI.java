package oncue.api.legacy;

import java.util.Map;

import oncue.common.messages.Job;


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
		// TODO Auto-generated method stub
		return null;
	}
	
	// Re-implement this>	
//		@Override
//		public Job enqueueJob(String workerType) throws APIException {
//			return RedisBackingStore.createJob(workerType, null);
//		}
	//
//		@Override
//		public Job enqueueJob(String workerType, Map<String, String> jobParams) throws APIException {
//			return RedisBackingStore.createJob(workerType, jobParams);
//		}	

	@Override
	public Job enqueueJob(String workerType, Map<String, String> jobParams)
			throws APIException {
		// TODO Auto-generated method stub
		return null;
	}

}
