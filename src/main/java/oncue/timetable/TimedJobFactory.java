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
public class TimedJobFactory {

	public static void createJobsFromJobMap(ActorSystem system, List<Map<String, Object>> jobList) {
		for (Map<String, Object> jobMap : jobList) {
			String name = (String) jobMap.get("name");
			String workerType = (String) jobMap.get("type");
			String endpointUri = (String) jobMap.get("endpointUri");

			Map<String, String> parameters = null;
			if(jobMap.keySet().contains("parameters")) {
				parameters = (Map<String, String>) jobMap.get("parameters");
			}

			createTimedJob(system, workerType, name, endpointUri, parameters);
		}
	}

	public static void createTimedJob(ActorSystem system, final String workerType,
			final String jobName, final String endpointUri, final Map<String, String> parameters) {
		system.actorOf(new Props(new UntypedActorFactory() {

			private static final long serialVersionUID = 1L;

			@Override
			public Actor create() throws Exception {
				return new TimedJob(workerType, endpointUri, parameters);
			}
		}), "job-timer-" + jobName);
	}
}
