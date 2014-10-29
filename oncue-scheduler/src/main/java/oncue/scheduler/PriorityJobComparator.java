package oncue.scheduler;

import java.util.Comparator;

import oncue.common.messages.Job;

/**
 * This Comparator will allow sorting Collections based on their priority. If two jobs have the same
 * priority, the older job will be higher up the sorted result than the newer job. If two jobs have
 * the same priority AND enqueueing time, the one with the lower ID will appear first.
 */
public class PriorityJobComparator implements Comparator<Job> {

	@Override
	public int compare(Job j1, Job j2) {
		int j1Priority = getPriority(j1);
		int j2Priority = getPriority(j2);

		// Sort is stable with respect to job enqueued time
		if (j1Priority == j2Priority) {
			int enqueueTimeComparison = j1.getEnqueuedAt().compareTo(j2.getEnqueuedAt());
			if (enqueueTimeComparison == 0) {
				// If they have the exact same time, i.e. same millisecond (DateTime provides
				// millisecond precision) the comparator will violate the Set contract as
				// TreeSet/TreeMap.contains stops searching when j1.compareTo(j2) == 0 but j1 == j2
				// will be false. In this case we'll rely on the ID being unique.
				return Long.compare(j1.getId(), j2.getId());
			} else {
				return enqueueTimeComparison;
			}
		} else {
			return -1 * Integer.compare(j1Priority, j2Priority);
		}
	}

	private int getPriority(Job j) {
		if (j.getParams() == null) {
			return 0;
		}
		String priority = j.getParams().get("priority");
		if (priority == null) {
			return 0;
		}
		return Integer.parseInt(priority);
	}
}