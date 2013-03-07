package oncue.timetable;

import akka.camel.javaapi.UntypedConsumerActor;

/**
 * A TimedJob is created from an entry in the timetable in the configuration
 * file. It will enqueue the specified job according to the specified Quartz
 * schedule.
 * 
 * See http://camel.apache.org/quartz.html for URI specifications
 */
public class TimedJob extends UntypedConsumerActor {

	private String workerType;
	private String schedule;

	public TimedJob(String workerType, String schedule) {
		this.workerType = workerType;
		this.schedule = schedule;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		System.out.println("yo");
	}

	@Override
	public String getEndpointUri() {
		return schedule;
	}

}
