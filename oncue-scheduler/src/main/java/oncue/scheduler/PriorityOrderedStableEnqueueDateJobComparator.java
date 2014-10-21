package oncue.scheduler;

import java.util.Comparator;

import oncue.common.messages.Job;

public class PriorityOrderedStableEnqueueDateJobComparator implements Comparator<Job> {

	@Override
	public int compare(Job j1, Job j2) {
		int j1Priority = getPriority(j1);
		int j2Priority = getPriority(j2);

		// Sort is stable with respect to job enqueued time
		if (j1Priority == j2Priority) {
			return j1.getEnqueuedAt().compareTo(j2.getEnqueuedAt());
		} else {
			return -1 * Integer.compare(j1Priority, j2Priority);
		}
	}

	private int getPriority(Job j) {
		String priority = j.getParams().get("priority");
		if (priority == null) {
			return 0;
		}
		return Integer.parseInt(priority);
	}
}