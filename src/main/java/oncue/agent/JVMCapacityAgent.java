package oncue.agent;

import oncue.agent.internal.AbstractAgent;
import oncue.messages.JVMCapacityWorkRequest;

/**
 * This agent will report a snapshot of the JVM memory capacity of this node
 * when requesting work.
 */
public class JVMCapacityAgent extends AbstractAgent {

	// Only used for testing
	private long testCapacity;

	public JVMCapacityAgent() {
	}

	public JVMCapacityAgent(long testCapacity) {
		this.testCapacity = testCapacity;

	}

	@Override
	protected void requestWork() {
		if (testProbe != null) {
			getScheduler().tell(new JVMCapacityWorkRequest(getSelf(), testCapacity, testCapacity, testCapacity),
					getSelf());
			return;
		}

		long freeMemory = Runtime.getRuntime().freeMemory();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long maxMemory = Runtime.getRuntime().maxMemory();

		getScheduler().tell(new JVMCapacityWorkRequest(getSelf(), freeMemory, totalMemory, maxMemory), getSelf());
	}

}
