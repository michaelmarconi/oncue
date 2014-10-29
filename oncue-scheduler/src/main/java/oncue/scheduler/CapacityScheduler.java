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
			for (String key : (List<String>) value.get("uniqueness-keys").unwrapped()) {
				keys.add(key);
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
			if (job.getId() == 6) {
				System.out.println("foo");
			}
			if (workRequest.getWorkerTypes().contains(job.getWorkerType())) {
				Map<String, String> params = job.getParams();
				int requiredMemory = getRequiredMemory(job);
				if (requiredMemory + allocatedMemory <= workRequest.getAvailableMemory()) {
					if (workerTypesToUniqueParameters.containsKey(job.getWorkerType())) {
						if (runningUniquenessConstrainedJobTypes.containsKey(job.getWorkerType())) {
							boolean foundIdentical = false;
							for (Map<String, String> parameterKeyValues : runningUniquenessConstrainedJobTypes
									.get(job.getWorkerType())) {
								if(foundIdentical) {
									break;
								}
								boolean identical = true;
								for (String parameter : parameterKeyValues.keySet()) {
									if (!params.get(parameter).equals(
											parameterKeyValues.get(parameter))) {
										identical = false;
										break;
									}
								}
								if (identical) {
									foundIdentical = true;
									break;
								}
							}
							if(!foundIdentical) {
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

	@Override
	protected void augmentJob(Job job) {
		ensureRequiredMemory(job);
	}

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

	private int getRequiredMemory(Job job) {
		return Integer.parseInt(job.getParams().get("memory"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Set<Map<String, String>>> getScheduledUniquenessConstrainedParams() {
		List<Job> scheduledJobs = getScheduledJobs();
		Map<String, Set<Map<String, String>>> scheduledUniquenessConstrainedParams = Maps
				.newHashMap();
		for (Job job : scheduledJobs) {
			if (workerTypesToUniqueParameters.containsKey(job.getWorkerType())) {
				Set<String> uniqueParams = workerTypesToUniqueParameters.get(job.getWorkerType());
				Map<String, String> parameters = Maps.newHashMap();
				for (String parameter : job.getParams().keySet()) {
					if (uniqueParams.contains(parameter)) {
						parameters.put(parameter, job.getParams().get(parameter));
					}
				}
				if (scheduledUniquenessConstrainedParams.containsKey(job.getWorkerType())) {
					scheduledUniquenessConstrainedParams.get(job.getWorkerType()).add(parameters);
				} else {
					scheduledUniquenessConstrainedParams.put(job.getWorkerType(),
							Sets.newHashSet(parameters));
				}
			}
		}
		return scheduledUniquenessConstrainedParams;
	}

}