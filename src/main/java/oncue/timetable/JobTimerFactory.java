package oncue.timetable;

import java.util.List;
import java.util.Map;

import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

/**
 * Created TimedJobs for all jobs specified in the job map.
 */
public class JobTimerFactory {

	public static void createJobsFromJobMap(ActorSystem system, List<Map<String, String>> jobList) {
		for (Map<String, String> jobMap : jobList) {
			String name = jobMap.get("name");
			String workerType = jobMap.get("type");
			String endpointUri = jobMap.get("endpointUri");
			createTimedJob(system, workerType, name, endpointUri);
		}
	}

	public static void createTimedJob(ActorSystem system, final String workerType,
			final String jobName, final String endpointUri) {
		system.actorOf(new Props(new UntypedActorFactory() {

			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new JobTimer(workerType, endpointUri);
			}
		}), "job-timer-" + jobName);
	}
}
