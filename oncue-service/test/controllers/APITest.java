package controllers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import static play.test.Helpers.stop;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.serializers.ObjectMapperFactory;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.SerializationConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeApplication;

public class APITest {
	private static FakeApplication fakeApplication;
	private final static ObjectMapper mapper = ObjectMapperFactory.getInstance();
	static {
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
		mapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
	}

	private DateTime expectedEnqueuedAt;

	@Before
	public void startFakeApplication() {
		Map<String, Object> extraConfig = new HashMap<>();
		extraConfig.put("oncue.scheduler.backing-store.class", "oncue.backingstore.InMemoryBackingStore");
		fakeApplication = fakeApplication(extraConfig);
		start(fakeApplication);
		DateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 03, 27, 12, 34, 56).getMillis());
		expectedEnqueuedAt = new DateTime(2013, 03, 27, 12, 34, 56, DateTimeZone.forTimeZone(TimeZone.getDefault()));
	}

	@After
	public void shutdownFakeApplication() {
		stop(fakeApplication);
		DateTimeUtils.setCurrentMillisSystem();
	}

	@Test
	public void listJobsButNoneHaveBeenQueued() throws JsonParseException, JsonMappingException, IOException {
		Result result = route(fakeRequest(GET, "/api/jobs"));

		assertThat(status(result)).isEqualTo(OK);
		assertThat(contentType(result)).isEqualTo("application/json");
		assertThat(charset(result)).isEqualTo("utf-8");

		List<?> jobs = mapper.readValue(contentAsString(result), ArrayList.class);
		assertTrue("There should be no jobs", jobs.size() == 0);
	}

	@Test
	public void listJobsWithOneQueued() throws JsonParseException, JsonMappingException, IOException {
		EnqueueJob enqueueJob = new EnqueueJob("oncue.test.TestWorker");
		routeAndCall(fakeRequest(POST, "/api/jobs").withJsonBody(mapper.valueToTree(enqueueJob)));

		Result result = route(fakeRequest(GET, "/api/jobs"));

		assertThat(status(result)).isEqualTo(OK);
		assertThat(contentType(result)).isEqualTo("application/json");
		assertThat(charset(result)).isEqualTo("utf-8");

		@SuppressWarnings("unchecked")
		List<Job> jobs = mapper.readValue(contentAsString(result), ArrayList.class);
		assertTrue("There should be one job", jobs.size() == 1);
	}

	@Test
	public void createJobWithNoParameters() throws JsonParseException, JsonMappingException, IOException {
		EnqueueJob enqueueJob = new EnqueueJob("oncue.test.TestWorker");

		/*
		 * TODO: migrate to the 'route' method when we move to Play 2.1.1, which
		 * fixes a bug in the Json payload delivery that causes this test to
		 * fail!
		 * 
		 * Result result = route(fakeRequest(POST, "/api/jobs").withJsonBody(
		 * Json.toJson(enqueueJob)));
		 */

		Result result = routeAndCall(fakeRequest(POST, "/api/jobs").withJsonBody(mapper.valueToTree(enqueueJob)));

		assertEquals(OK, status(result));
		assertEquals("application/json", contentType(result));
		assertEquals("utf-8", charset(result));

		Job job = mapper.readValue(contentAsString(result), Job.class);

		assertEquals("oncue.test.TestWorker", job.getWorkerType());
		assertTrue(expectedEnqueuedAt.isEqual(job.getEnqueuedAt()));
		assertNotNull(job.getId());
		assertEquals(0.0, job.getProgress());
		assertTrue(job.getParams().isEmpty());
	}

	@Test
	public void createJobWithParameters() throws JsonParseException, JsonMappingException, IOException {
		Map<String, String> params = new HashMap<>();
		params.put("key1", "Value 1");
		params.put("key2", "Value 2");
		EnqueueJob enqueueJob = new EnqueueJob("oncue.test.TestWorker", params);

		/*
		 * TODO: migrate to the 'route' method when we move to Play 2.1.1, which
		 * fixes a bug in the Json payload delivery that causes this test to
		 * fail!
		 * 
		 * Result result = route(fakeRequest(POST, "/api/jobs").withJsonBody(
		 * Json.toJson(enqueueJob)));
		 */

		Result result = routeAndCall(fakeRequest(POST, "/api/jobs").withJsonBody(mapper.valueToTree(enqueueJob)));

		assertEquals(OK, status(result));
		assertEquals("application/json", contentType(result));
		assertEquals("utf-8", charset(result));

		Job job = mapper.readValue(contentAsString(result), Job.class);

		assertEquals("oncue.test.TestWorker", job.getWorkerType());
		assertTrue(expectedEnqueuedAt.isEqual(job.getEnqueuedAt()));
		assertNotNull(job.getId());
		assertEquals(0.0, job.getProgress());
		assertEquals("Value 1", job.getParams().get("key1"));
		assertEquals("Value 2", job.getParams().get("key2"));
	}

	@Test
	public void rerunJob() throws JsonParseException, JsonMappingException, IOException {
		EnqueueJob enqueueJob = new EnqueueJob("oncue.test.TestWorker");

		/*
		 * TODO: migrate to the 'route' method when we move to Play 2.1.1, which
		 * fixes a bug in the Json payload delivery that causes this test to
		 * fail!
		 * 
		 * Result result = route(fakeRequest(POST, "/api/jobs").withJsonBody(
		 * Json.toJson(enqueueJob)));
		 */

		Result result = routeAndCall(fakeRequest(POST, "/api/jobs").withJsonBody(mapper.valueToTree(enqueueJob)));
		Job job = mapper.readValue(contentAsString(result), Job.class);

//		result = routeAndCall(fakeRequest(PUT, "/api/jobs/" + job.getId()));
//		Job rerunJob = mapper.readValue(contentAsString(result), Job.class);
//
//		assertEquals(OK, status(result));
//		assertEquals("application/json", contentType(result));
//		assertEquals("utf-8", charset(result));
//
//		assertEquals("oncue.test.TestWorker", rerunJob.getWorkerType());
//		assertTrue(expectedEnqueuedAt.isEqual(rerunJob.getEnqueuedAt()));
//		assertEquals(job.getId(), rerunJob.getId());
//		assertEquals(0.0, rerunJob.getProgress());
//		assertTrue(job.getParams().isEmpty());
//		assertTrue(rerunJob.isRerun() == true);
	}

}
