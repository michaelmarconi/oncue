package oncue.tests;

import java.lang.reflect.Constructor;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import oncue.common.Injectable;

public class Creators {
	private Creators() {
	}

	public static <T extends Injectable> Creator<T> makeCreator(ActorRef probe, Class<T> actorClass,
			Object... args) {
		return new CreatorImpl<>(probe, actorClass, args);
	}

	public static <T extends Injectable & Actor> Props makeProps(ActorRef probe, Class<T> actorClass,
			Object... args) {
		return Props.create(Creators.makeCreator(probe, actorClass, args));
	}

	private static class CreatorImpl<T extends Injectable> implements Creator<T> {
		private ActorRef probe;
		private Class<T> actorClass;
		private Object[] args;

		public CreatorImpl(ActorRef probe, Class<T> actorClass, Object[] args) {
			this.probe = probe;
			this.actorClass = actorClass;
			this.args = args;
		}

		@SuppressWarnings("unchecked")
		private Constructor<T> findCtor() {
			for (Constructor<?> ctor : actorClass.getConstructors()) {
				Class<?>[] paramTypes = ctor.getParameterTypes();
				if (args.length != paramTypes.length) {
					continue;
				}
				int i;
				for (i = 0; i < args.length; ++i) {
					Object arg = args[i];
					if (arg == null && paramTypes[i].isPrimitive()) {
						break;
					}
					if (!paramTypes[i].isAssignableFrom(arg.getClass())) {
						break;
					}
				}
				if (i == args.length) {
					return (Constructor<T>) ctor;
				}
			}
			throw new RuntimeException("Constructor not found");
		}

		@Override
		public T create() throws Exception {
			T result = findCtor().newInstance(args);
			result.injectProbe(probe);
			return result;
		}
	}

}
