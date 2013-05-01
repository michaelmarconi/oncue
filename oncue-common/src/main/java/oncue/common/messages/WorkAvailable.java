package oncue.common.messages;

import java.io.Serializable;
import java.util.Set;

/**
 * When work is waiting to be scheduled, the Scheduler will advertise this work
 * to all agents using this message. The type of workers required to perform
 * this work will be included, so the Agent can decide whether to request work.
 */
public class WorkAvailable implements Serializable {

	private static final long serialVersionUID = 7254357873799337038L;

	private final Set<String> workerTypes;

	public WorkAvailable(Set<String> workerTypes) {
		super();
		this.workerTypes = workerTypes;
	}

	public Set<String> getWorkerTypes() {
		return workerTypes;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (String workerType : workerTypes) {
			builder.append(workerType);
			builder.append(" ");
		}
		return builder.toString();
	}
}
