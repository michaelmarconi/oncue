package oncue.tests;

import static org.junit.Assert.fail;
import oncue.tests.base.ActorSystemTest;

import org.junit.Ignore;
import org.junit.Test;

/**
 * A series of tests to ensure that a job goes through the proper state
 * transitions as it is queued, scheduled, run, etc.
 */
public class JobStateTest extends ActorSystemTest {

	@Test
	@Ignore
	public void jobIsQueued() {
		fail("Not implemented!");
	}

}
