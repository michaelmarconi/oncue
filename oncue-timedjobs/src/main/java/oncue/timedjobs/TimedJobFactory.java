package oncue.timedjobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

/**
 * Create timed jobs for all jobs specified in the job map.
 */
public class TimedJobFactory {

	@SuppressWarnings("unchecked")
	public static void createTimedJobs(ActorSystem system, List<Map<String, Object>> jobList, ActorRef testingProbe) {
		for (Map<String, Object> jobMap : jobList) {
			String name = (String) jobMap.get("name");
			String workerType = (String) jobMap.get("type");
			String endpointUri = (String) jobMap.get("endpointUri");

			Integer failureRetryCount = null;
			if (jobMap.keySet().contains("failureRetryCount")) {
				failureRetryCount = (Integer) jobMap.get("failureRetryCount");
			}

			Map<String, String> parameters = new HashMap<>();
			if (jobMap.keySet().contains("parameters")) {
				Map<String, Object> jobParams = (Map<String, Object>) jobMap.get("parameters");
				for (String key : jobParams.keySet()) {
					parameters.put(key, jobParams.get(key).toString());
				}
			}

			createTimedJob(system, workerType, name, endpointUri, parameters, failureRetryCount, testingProbe);
		}
	}

	public static void createTimedJobs(ActorSystem system, List<Map<String, Object>> jobList) {
		createTimedJobs(system, jobList, null);
	}

	@SuppressWarnings("serial")
	public static void createTimedJob(ActorSystem system, final String workerType, final String jobName,
			final String endpointUri, final Map<String, String> parameters, final Integer failureRetryCount,
			final ActorRef testProbe) {

		system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				return new TimedJob(workerType, endpointUri, parameters, failureRetryCount, testProbe);

			}
		}), "job-timer-" + jobName);
	}

	public static void createTimedJob(ActorSystem system, final String workerType, final String jobName,
			final String endpointUri, final Map<String, String> parameters, ActorRef testProbe) {
		createTimedJob(system, workerType, jobName, endpointUri, parameters, null, testProbe);
	}

	public static void createTimedJob(ActorSystem system, final String workerType, final String jobName,
			final String endpointUri, final Map<String, String> parameters) {
		createTimedJob(system, workerType, jobName, endpointUri, parameters, null, null);
	}
}
