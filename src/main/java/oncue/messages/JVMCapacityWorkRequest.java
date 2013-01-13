package oncue.messages;

import oncue.messages.internal.AbstractWorkRequest;
import akka.actor.ActorRef;

public class JVMCapacityWorkRequest extends AbstractWorkRequest {

	private long freeMemory;
	private long totalMemory;
	private long maxMemory;

	private static final long serialVersionUID = -4503173564405936817L;

	public JVMCapacityWorkRequest(ActorRef agent, long freeMemory, long totalMemory, long maxMemory) {
		super(agent);
		this.setFreeMemory(freeMemory);
		this.totalMemory = totalMemory;
		this.maxMemory = maxMemory;
	}

	public long getFreeMemory() {
		return freeMemory;
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}

	@Override
	public String toString() {
		return super.toString()
				+ String.format(" [freeMem = %s, totalMem = %s, maxMem = %s]", getFreeMemory(), totalMemory, maxMemory);
	}
}
