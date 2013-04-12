package oncue.common.events;

import java.io.Serializable;

import oncue.common.messages.Agent;

/**
 * This event is fired when an agent has stopped and is no longer available.
 */
public class AgentStopped implements Serializable {

	private static final long serialVersionUID = -866097369897591277L;
	private Agent agent;

	public AgentStopped(Agent agent) {
		this.agent = agent;
	}

	public Agent getAgent() {
		return agent;
	}

	@Override
	public String toString() {
		return agent.getUrl();
	}
}
