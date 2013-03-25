//package oncue.tests.api;
//
//import static junit.framework.Assert.assertEquals;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import junit.framework.Assert;
//import oncue.messages.internal.Job;
//import oncue.service.backingstore.RedisBackingStore;
//import oncue.tests.workers.TestWorker;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import redis.clients.jedis.Jedis;
//
//public class RedisAPITests {
//
//	@Before
//	public void setUp() throws Exception {
//		RedisBackingStore.getConnection().flushAll();
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		RedisBackingStore.getConnection().flushAll();
//	}
//
//	@Test
//	public void enqueueJobWithoutParams() throws APIException {
//
//		// Enqueue a new job
//		Job job = RedisAPI.getInstance().enqueueJob(TestWorker.class.getName());
//
//		Jedis redis = RedisBackingStore.getConnection();
//		Job persistedJob = RedisBackingStore.loadJob(job.getId(), redis);
//
//		assertEquals(job.getId(), persistedJob.getId());
//		assertEquals(job.getWorkerType(), persistedJob.getWorkerType());
//		Assert.assertNull(persistedJob.getParams());
//	}
//
//	@Ignore
//	@Test
//	public void enqueueJobWithParams() throws APIException {
//
//		// Create some params
//		Map<String, String> params = new HashMap<>();
//		params.put("Colour", "Deep purple");
//
//		// Enqueue a new job
//		Job job = RedisAPI.getInstance().enqueueJob(TestWorker.class.getName(), params);
//
//		Jedis redis = RedisBackingStore.getConnection();
//		Job persistedJob = RedisBackingStore.loadJob(job.getId(), redis);
//
//		assertEquals(job.getId(), persistedJob.getId());
//		assertEquals(job.getWorkerType(), persistedJob.getWorkerType());
//		assertEquals(job.getParams().get("Colour"), persistedJob.getParams().get("Colour"));
//	}
//
//}
