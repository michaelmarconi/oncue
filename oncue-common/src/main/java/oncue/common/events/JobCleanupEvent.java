package oncue.common.events;

import java.io.Serializable;

/**
 * This event is fired when the maintenance worker has cleaned up the
 * completed/failed jobs.
 */
public class JobCleanupEvent implements Serializable {

	private static final long serialVersionUID = 5037062135239309149L;

	public JobCleanupEvent() {
	}

}