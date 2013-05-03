package oncue.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.serializers.ObjectMapperFactory;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

class HttpClient implements Client {

	private static GenericUrl enqueueJobUrl;
	private static ObjectMapper mapper;

	static {
		Config config = ConfigFactory.load();
		String hostName = config.getString("oncue.service.hostname");
		String port = config.getString("oncue.service.port");
		String basePath = config.getString("oncue.service.base-url-path");
		String enqueueJobUrlString = String.format("http://%s:%s%s/jobs", hostName, port, basePath);
		enqueueJobUrl = new GenericUrl(enqueueJobUrlString);
		mapper = ObjectMapperFactory.getInstance();
	}

	private HttpRequestFactory requestFactory;

	HttpClient(HttpTransport transport) {
		requestFactory = transport.createRequestFactory();
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
			ByteArrayContent content = new ByteArrayContent("application/json", mapper.writeValueAsBytes(job));
			HttpRequest request = requestFactory.buildPostRequest(enqueueJobUrl, content);
			HttpResponse response = request.execute();
			return parseJob(response);
		} catch (IOException e) {
			throw new ClientException("Error enqueueing job.", e);
		}
	}

	private Job parseJob(HttpResponse response) throws ClientException {
		try(InputStream content = response.getContent()) {
			return mapper.readValue(content, Job.class);
		} catch (JsonMappingException e) {
			throw new ClientException("Invalid response body", e);
		} catch (IOException e) {
			throw new ClientException(e);
		}
	}
}
