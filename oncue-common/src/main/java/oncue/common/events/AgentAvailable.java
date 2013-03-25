package oncue.common.events;

import java.io.Serializable;

public class AgentAvailable implements Serializable {

	private static final long serialVersionUID = -4209324707640249480L;
	private String agent;

	public AgentAvailable(String agent) {
		this.agent = agent;
	}

	public String getAgent() {
		return agent;
	}

	@Override
	public String toString() {
		return agent;
	}
}
