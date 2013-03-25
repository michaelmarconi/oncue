package oncue.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import oncue.common.messages.Job;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.client.util.StreamingContent;

public class HttpClientTest {

	private static String sampleResponse = "{\"enqueuedAt\" : \"2013-03-23T12:13:14+00:00\",\"workerType\" : \"com.example.ExampleWorker\",\"id\" : 2,\"params\" : {\"key1\" : \"Value 1\",\"key2\" : \"Value 2\"},\"progress\" : 0.5  }";

	private static JsonFactory jackson = new JsonFactory();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void clientPostsToConfiguredUrl() throws ClientException {
		ClientMockTransport transport = new ClientMockTransport(200,
				sampleResponse);
		Client client = new HttpClient(transport);
		client.enqueueJob("com.example.SubmittedJob");
		
		Assert.assertEquals("http://localhost:9000/api/jobs", transport.getUrl()); 
	}

	@Test
	public void clientPostsWorkerNameInRequestBody() throws ClientException, IOException {
		ClientMockTransport transport = new ClientMockTransport(200,
				sampleResponse);
		Client client = new HttpClient(transport);
		client.enqueueJob("com.example.SubmittedJob");
		Map<String, Object> requestMap = transport.getRequestContentAsMap();
		
		Assert.assertEquals("com.example.SubmittedJob", (String)requestMap.get("workerType")); 
	}
	
	@Test
	public void clientPostsEmptyParamsInBody() throws ClientException, IOException {
		ClientMockTransport transport = new ClientMockTransport(200,
				sampleResponse);
		Client client = new HttpClient(transport);
		client.enqueueJob("com.example.SubmittedJob");
		Map<String, Object> requestMap = transport.getRequestContentAsMap();
		
		assertNotNull(requestMap.get("params"));
		assertTrue(requestMap.get("params") instanceof Map);
		assertTrue(((Map)requestMap.get("params")).isEmpty());
	}
	
	@Test
	public void clientPostsNonEmptyParamsInBody() throws ClientException, IOException {
		ClientMockTransport transport = new ClientMockTransport(200,
				sampleResponse);
		Client client = new HttpClient(transport);

		Map<String, String> params = new HashMap<>();
		params.put("key1", "value1");
		params.put("key2", "value2");
		
		client.enqueueJob("com.example.SubmittedJob", params);
		Map<String, Object> requestMap = transport.getRequestContentAsMap();

		
		assertNotNull(requestMap.get("params"));
		assertTrue(requestMap.get("params") instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, String> bodyParams = (Map<String, String>) requestMap.get("params");
		assertEquals(params, bodyParams);
	}

	@Test
	public void clientParsesValidResponseIntoJob() throws ClientException, IOException {
		ClientMockTransport transport = new ClientMockTransport(200,
				sampleResponse);
		Client client = new HttpClient(transport);
		
		Job job = client.enqueueJob("com.example.SubmittedJob");


		assertNotNull(job);
		assertEquals(2, job.getId());
		assertTrue(new DateTime(2013, 3, 23, 12, 13, 14, DateTimeZone.forID("+00:00")).compareTo(job.getEnqueuedAt()) ==0);
		assertEquals(0.5, (double)job.getProgress(), 0);
		assertEquals("com.example.ExampleWorker", job.getWorkerType());
		assertNotNull(job.getParams());
		assertEquals(2, job.getParams().size());
		assertEquals("Value 1", job.getParams().get("key1"));
		assertEquals("Value 2", job.getParams().get("key2"));
	}
	
	@Test
	public void clientThrowsExceptionWhenInvalidResponseBodyReturned() throws ClientException, IOException {
		ClientMockTransport transport = new ClientMockTransport(200,
				"nonsense");
		Client client = new HttpClient(transport);

		expectedException.expect(ClientException.class);
		client.enqueueJob("com.example.SubmittedJob");
	}
	
	@Test
	public void clientThrowsExceptionWhenNon200ResponseCodeReturned() throws ClientException, IOException {
		ClientMockTransport transport = new ClientMockTransport(404,
				sampleResponse);
		Client client = new HttpClient(transport);
		
		expectedException.expect(ClientException.class);
		client.enqueueJob("com.example.SubmittedJob");
	}
	
	
	public class ClientMockTransport extends MockHttpTransport {

		private int responseCode;
		private String responseBody;
		private String method;
		private String url;
		private MockLowLevelHttpRequest request;

		ClientMockTransport(int responseCode, String responseBody) {
			this.responseCode = responseCode;
			this.responseBody = responseBody;
		}

		@Override
		public LowLevelHttpRequest buildRequest(String method, String url)
				throws IOException {
			this.method = method;
			this.url = url;
			request = new MockLowLevelHttpRequest() {
				@Override
				public LowLevelHttpResponse execute() throws IOException {
					MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
					response.setStatusCode(responseCode);
					response.setContentType(Json.MEDIA_TYPE);
					response.setContent(responseBody);
					return response;
				}
			};
			return request;
		}

		public String getMethod() {
			return method;
		}

		public String getUrl() {
			return url;
		}

		public String getRequestContent() throws IOException {
			StreamingContent streamingContent = request.getStreamingContent();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			streamingContent.writeTo(stream);
			return new String(stream.toByteArray());
		}
		
		public Map<String, Object> getRequestContentAsMap() throws JsonParseException, JsonMappingException, IOException {
		    JsonFactory factory = new JsonFactory(); 
		    ObjectMapper mapper = new ObjectMapper(factory); 
		    TypeReference<HashMap<String,Object>> typeRef 
		          = new TypeReference< 
		                 HashMap<String,Object> 
		               >() {}; 
		    return mapper.readValue(getRequestContent(), typeRef); 
			
		}
	}

}
