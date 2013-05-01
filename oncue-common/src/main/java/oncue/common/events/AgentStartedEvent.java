package oncue.common.events;

import java.io.Serializable;

import oncue.common.messages.Agent;

/**
 * This event is fired when an agent has started and becomes available.
 */
public class AgentStartedEvent implements Serializable {

	private static final long serialVersionUID = -4209324707640249480L;
	private Agent agent;

	public AgentStartedEvent(Agent agent) {
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
