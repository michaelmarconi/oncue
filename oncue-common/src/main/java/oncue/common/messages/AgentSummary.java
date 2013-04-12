package oncue.common.messages;

import java.io.Serializable;
import java.util.List;

/**
 * A scheduler will send this message in response to a request to list
 * registered agents.
 */
public class AgentSummary implements Serializable {

	private static final long serialVersionUID = 4123568243352990886L;
	private List<Agent> agents;

	public AgentSummary(List<Agent> agents) {
		super();
		this.agents = agents;
	}

	public List<Agent> getAgents() {
		return agents;
	}

	public void setAgents(List<Agent> agents) {
		this.agents = agents;
	}
}
