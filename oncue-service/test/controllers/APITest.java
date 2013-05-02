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
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.common.serializers.ObjectMapperFactory;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeApplication;

public class APITest {
	private final static FakeApplication fakeApplication = fakeApplication();
	private final static ObjectMapper mapper = ObjectMapperFactory.getInstance();
	private DateTime expectedEnqueuedAt;

	@Before
	public void startFakeApplication() {
		start(fakeApplication);
		DateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 03, 27, 12, 34, 56).getMillis());
	}

	@After
	public void shutdownFakeApplication() {
		stop(fakeApplication);
		DateTimeUtils.setCurrentMillisSystem();
	}

	@Before
	public void setUp() {
		expectedEnqueuedAt = new DateTime(2013, 03, 27, 12, 34, 56, DateTimeZone.forTimeZone(TimeZone.getDefault()));
	}

	@Test
	public void listJobsButNoneHaveBeenQueued() throws JsonParseException, JsonMappingException, IOException {
		Result result = route(fakeRequest(GET, "/api/jobs"));

		assertThat(status(result)).isEqualTo(OK);
		assertThat(contentType(result)).isEqualTo("application/json");
		assertThat(charset(result)).isEqualTo("utf-8");

		JobSummary jobSummary = mapper.readValue(contentAsString(result), JobSummary.class);
		assertTrue("There should be no jobs", jobSummary.getJobs().size() == 0);
	}

	@Ignore("Not yet implemented")
	@Test
	public void listJobsWithOneQueued() throws JsonParseException, JsonMappingException, IOException {
		Result result = route(fakeRequest(GET, "/api/jobs"));

		assertThat(status(result)).isEqualTo(OK);
		assertThat(contentType(result)).isEqualTo("application/json");
		assertThat(charset(result)).isEqualTo("utf-8");

		JobSummary jobSummary = mapper.readValue(contentAsString(result), JobSummary.class);
		assertTrue("There should be one job", jobSummary.getJobs().size() == 1);
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

	/**
	 * TODO: Kill the QM and restart it before each test, so run IDs stay
	 * constant!
	 */
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

}
