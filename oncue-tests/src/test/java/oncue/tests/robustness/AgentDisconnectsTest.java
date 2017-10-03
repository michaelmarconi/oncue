package oncue.tests.robustness;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import oncue.agent.UnlimitedCapacityAgent;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.messages.WorkResponse;
import oncue.tests.Creators;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.PerpetualTestWorker;
import oncue.tests.workers.TestWorker;

public class AgentDisconnectsTest extends ActorSystemTest {

	public AgentDisconnectsTest() {
		this.waitForRunningJobs = false;
	}

	/**
	 * This test simulates an agent failing to send a heartbeat to the Scheduler in the required
	 * amount of time. This causes the scheduler to assume that the agent has died and to give the
	 * job that was in progress to the next compatible agent that comes along.
	 */
	@SuppressWarnings("serial")
	@Test
	public void testAgentDisconnectsAndReconnects() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								if (message.equals(SimpleMessage.AGENT_DEAD)
										|| message instanceof WorkResponse)
									return false;
								else
									return true;
							}
						};
					}
				};

				// Create a scheduler with a probe
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Create an agent probe
				final JavaTestKit agentProbe = new JavaTestKit(system) {

					{
						new IgnoreMsg() {

							protected boolean ignore(Object message) {
								return !(message instanceof WorkResponse);
							}
						};
					}
				};

				// Create a naked agent
				final Props agentProps = Creators.makeProps(agentProbe.getRef(), UnlimitedCapacityAgent.class,
						new HashSet<>(Arrays.asList(PerpetualTestWorker.class.getName(),
								TestWorker.class.getName())));

				final TestActorRef<UnlimitedCapacityAgent> agentRef = TestActorRef.create(system,
						agentProps, settings.AGENT_NAME);

				// Enqueue a job
				scheduler.tell(new EnqueueJob(PerpetualTestWorker.class.getName()), getRef());

				expectMsgClass(Job.class);

				// Wait for the agent to be sent the job by the scheduler
				new AwaitCond(duration("5 seconds")) {
					@Override
					protected boolean cond() {
						WorkResponse response = agentProbe.expectMsgClass(WorkResponse.class);
						return !response.getJobs().isEmpty();
					}
				};

				agentRef.underlyingActor().stopHeartbeat();

				schedulerProbe.expectMsgEquals(duration("60 seconds"), SimpleMessage.AGENT_DEAD);

				agentRef.underlyingActor().startHeartbeat();

				// Wait for the scheduler to re-schedule the job
				agentProbe.expectMsgClass(duration("20 seconds"), WorkResponse.class);

				// Wait until we receive another JobProgress message. Without the guard in
				// AbstractAgent#spawnWorker that stops actors from being spawned if the job is in
				// progress an Exception would be thrown before this JobProgress message is
				// returned. This AwaitCond is a little odd because we don't have the latest version
				// of Akka that has the fishForMessage method.
				schedulerProbe.ignoreNoMsg();
				new AwaitCond(duration("5 seconds")) {

					@Override
					protected boolean cond() {
						@SuppressWarnings("unchecked")
						Object message = schedulerProbe.expectMsgAnyClassOf(JobProgress.class,
								WorkResponse.class);
						if (message instanceof WorkResponse) {
							return false;
						}
						return true;
					}

				};
			}
		};
	}
}
