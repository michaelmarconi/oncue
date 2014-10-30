/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package oncue.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oncue.backingstore.BackingStore;
import oncue.common.messages.CapacityWorkRequest;
import oncue.common.messages.Job;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

/**
 * A capacity-based scheduler.
 * 
 * Hands out jobs in priority order. This assumes a Job has a parameter value of "priority". If a
 * job does not provide a priority, it is assumed to be 0 (low priority).
 * 
 * This scheduler will only provide as many jobs to an agent as it declares it has free memory. See
 * the documentation for {@link CapacityAgent} for more information.
 * 
 * This scheduler also allows specification of a job that cannot be run in parallel. The worker type
 * must be specified with "oncue.scheduler.capacity-scheduler.worker-type" and the parameter for
 * jobs of this type that enforces uniqueness is configurable by
 * "oncue.scheduler.capacity-scheduler.uniqueness-parameter". I.e. two jobs with the worker type
 * specified by "worker-type" with the same parameter value for the property described by
 * "uniqueness-parameter" will not run at the same time.
 */
public class CapacityScheduler extends AbstractScheduler<CapacityWorkRequest> {

	private final Config config;
	private final Map<String, Set<String>> workerTypesToUniqueParameters;

	@SuppressWarnings("unchecked")
	public CapacityScheduler(Class<? extends BackingStore> backingStore) {
		super(backingStore);
		config = getContext().system().settings().config()
				.getConfig("oncue.scheduler.capacity-scheduler");

		List<? extends ConfigObject> uniquenessConstraints = config
				.getObjectList("uniqueness-constraints");
		workerTypesToUniqueParameters = Maps.newHashMap();
		for (ConfigObject value : uniquenessConstraints) {
			Set<String> keys = Sets.newHashSet();
			if (value.containsKey("uniqueness-keys")) {
				for (String key : (List<String>) value.get("uniqueness-keys").unwrapped()) {
					keys.add(key);
				}
			}
			workerTypesToUniqueParameters.put((String) value.get("worker-type").unwrapped(), keys);
		}
	}

	protected Comparator<Job> getComparator() {
		return new PriorityJobComparator();
	}

	@Override
	protected void scheduleJobs(CapacityWorkRequest workRequest) {
		Map<String, Set<Map<String, String>>> runningUniquenessConstrainedJobTypes = getScheduledUniquenessConstrainedParams();
		List<Job> jobs = new ArrayList<>();
		int allocatedMemory = 0;

		Iterator<Job> iterator = unscheduledJobs.iterator();

		while (iterator.hasNext()) {
			Job job = iterator.next();
			if (workRequest.getWorkerTypes().contains(job.getWorkerType())) {
				int requiredMemory = getRequiredMemory(job);
				if (requiredMemory + allocatedMemory <= workRequest.getAvailableMemory()) {
					if (workerTypesToUniqueParameters.containsKey(job.getWorkerType())) {
						if (runningUniquenessConstrainedJobTypes.containsKey(job.getWorkerType())) {
							boolean blockedByRunningUniquenessConstrainedJob = findRunningUniquenessConstraintedJobConflicts(
									runningUniquenessConstrainedJobTypes, job);
							if (!blockedByRunningUniquenessConstrainedJob) {
								addJob(job, runningUniquenessConstrainedJobTypes);
								jobs.add(job);
								allocatedMemory += requiredMemory;
							}
						} else {
							addJob(job, runningUniquenessConstrainedJobTypes);
							jobs.add(job);
							allocatedMemory += requiredMemory;
						}
					} else {
						jobs.add(job);
						allocatedMemory += requiredMemory;
					}
				}
			}
		}

		log.debug("Scheduling {} job(s) with a total memory of {}", jobs.size(), allocatedMemory);

		// Create the schedule
		Schedule schedule = new Schedule();
		schedule.setJobs(getSender(), jobs);

		// Dispatch the schedule
		dispatchJobs(schedule);
	}

	/**
	 * Check if the uniqueness parameters for the given job conflict with any existing running job's
	 * uniqueness parameters. I.e., given a map of worker type -> (set of (map of uniqueness key ->
	 * parameter value for that uniqueness key)), ensure that for each defined uniqueness key for
	 * the job's worker type there are no entries in the running job's uniquness key/value pairs
	 * that are identical.
	 * 
	 * @param runningUniquenessConstrainedJobTypes Information on the currently running jobs and
	 * their parameter values for all uniqueness constrained parameters
	 * @param job The job
	 * @return
	 */
	private boolean findRunningUniquenessConstraintedJobConflicts(
			Map<String, Set<Map<String, String>>> runningUniquenessConstrainedJobTypes, Job job) {
		boolean foundIdentical = false;
		for (Map<String, String> parameterKeyValues : runningUniquenessConstrainedJobTypes.get(job
				.getWorkerType())) {
			if (foundIdentical) {
				break;
			}
			boolean identical = true;
			for (String parameter : parameterKeyValues.keySet()) {
				if (!job.getParams().get(parameter).equals(parameterKeyValues.get(parameter))) {
					identical = false;
					break;
				}
			}
			if (identical) {
				foundIdentical = true;
				break;
			}
		}
		return foundIdentical;
	}

	@Override
	protected void augmentJob(Job job) {
		ensureRequiredMemory(job);
	}

	/**
	 * Ensure that a job has a "memory" parameter. If it exists, leave it as it is. If it does not,
	 * attempt to populate it from the configuration. This will crash if both the job does not have
	 * the parameter and the configuration does not define a default value for this type of job.
	 * 
	 * @param job The job
	 */
	private void ensureRequiredMemory(Job job) {
		Map<String, String> params = job.getParams();
		if (!params.containsKey("memory")) {
			Config config = getContext().system().settings().config();
			params.put(
					"memory",
					String.valueOf(config.getConfig("oncue.scheduler.capacity-scheduler").getInt(
							"default-requirements." + job.getWorkerType() + ".memory")));
		}
	}

	/**
	 * Get an integer representation of the memory key for a job. Caller should ensure jobs has a
	 * memory parameter.
	 * 
	 * @param job The job
	 * @return
	 */
	private int getRequiredMemory(Job job) {
		return Integer.parseInt(job.getParams().get("memory"));
	}

	/**
	 * Return a list of all scheduled jobs that have uniqueness constraints and the parameter
	 * key/values for keys defined in those uniqueness constraints
	 * 
	 * @return
	 */
	private Map<String, Set<Map<String, String>>> getScheduledUniquenessConstrainedParams() {
		List<Job> scheduledJobs = getScheduledJobs();
		Map<String, Set<Map<String, String>>> scheduledUniquenessConstrainedParams = Maps
				.newHashMap();
		for (Job job : scheduledJobs) {
			if (workerTypesToUniqueParameters.containsKey(job.getWorkerType())) {
				addJob(job, scheduledUniquenessConstrainedParams);
			}
		}
		return scheduledUniquenessConstrainedParams;
	}

	/**
	 * Add a job and it's uniqueness parameters to a list of active uniqueness constrained jobs.
	 * 
	 * @param job
	 * @param runningUniquenessConstrainedJobTypes
	 */
	private void addJob(Job job,
			Map<String, Set<Map<String, String>>> runningUniquenessConstrainedJobTypes) {
		Set<String> uniqueParams = workerTypesToUniqueParameters.get(job.getWorkerType());
		Map<String, String> uniqueParamValues = Maps.newHashMap();
		for (String key : job.getParams().keySet()) {
			if (uniqueParams.contains(key)) {
				uniqueParamValues.put(key, job.getParams().get(key));
			}
		}
		if (!runningUniquenessConstrainedJobTypes.containsKey(job.getWorkerType())) {
			Set<Map<String, String>> params = Sets.newHashSet();
			runningUniquenessConstrainedJobTypes.put(job.getWorkerType(), params);
		}

		runningUniquenessConstrainedJobTypes.get(job.getWorkerType()).add(uniqueParamValues);
	}

}