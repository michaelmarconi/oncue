package oncue.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;

import org.joda.time.DateTime;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

class HttpClient implements Client {

	private static GenericUrl enqueueJobUrl;

	static {
		Config config = ConfigFactory.load();
		String hostName = config.getString("oncue.service.hostname");
		String port = config.getString("oncue.service.port");
		String basePath = config.getString("oncue.service.base-url-path");
		String enqueueJobUrlString = String.format("http://%s:%s%s/jobs", hostName, port, basePath);
		enqueueJobUrl = new GenericUrl(enqueueJobUrlString);
	}

	private HttpRequestFactory requestFactory;
	private Gson gson;

	HttpClient(HttpTransport transport) {
		requestFactory = transport.createRequestFactory();
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeDeserializer());
		gson = gsonBuilder.create();
	}

	@Override
	public Job enqueueJob(String workerType) throws ClientException {
		return enqueueJob(workerType, Collections.<String, String> emptyMap());
	}

	@Override
	public Job enqueueJob(String workerType, Map<String, String> jobParams) throws ClientException {
		EnqueueJob job = new EnqueueJob(workerType, jobParams == null ? Collections.<String, String> emptyMap()
				: jobParams);
		try {
			ByteArrayContent content = new ByteArrayContent("application/json", toJson(job));
			HttpRequest request = requestFactory.buildPostRequest(enqueueJobUrl, content);
			HttpResponse response = request.execute();
			return parseJob(response);
		} catch (IOException e) {
			throw new ClientException("Error enqueueing job.", e);
		}
	}

	private Job parseJob(HttpResponse response) throws ClientException {
		try {
			InputStream content = response.getContent();
			Job job = gson.fromJson(new InputStreamReader(content), Job.class);
			if (job == null) {
				throw new ClientException("Invalid response body");
			}
			return job;
		} catch (Exception e) {
			throw new ClientException("Error reading response body");
		}

	}

	private byte[] toJson(EnqueueJob job) {
		return gson.toJson(job).getBytes();
	}

	private class DateTimeDeserializer implements JsonDeserializer<DateTime> {
		public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return new DateTime(json.getAsJsonPrimitive().getAsString());
		}
	}

}
