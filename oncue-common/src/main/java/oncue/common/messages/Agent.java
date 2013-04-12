package oncue.common.messages;

import java.io.Serializable;

/**
 * A simple data structure that represents an agent
 */
public class Agent implements Serializable {

	private static final long serialVersionUID = 1597787228823561798L;
	private String id;
	private String url;

	public Agent(String url) {
		super();
		this.id = url;
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}
}
