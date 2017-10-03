package oncue.common;

import akka.actor.ActorRef;

public interface Injectable {
	void injectProbe(ActorRef probe);
}
