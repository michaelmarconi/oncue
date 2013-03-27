package oncue.client;

import com.google.api.client.http.javanet.NetHttpTransport;

public class ClientFactory {

	private static HttpClient httpClient;

	public static Client getInstance() {
		if (httpClient == null) {
			httpClient = new HttpClient(new NetHttpTransport());
		}
		return httpClient;
	}

}
