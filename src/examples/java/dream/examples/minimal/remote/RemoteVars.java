package dream.examples.minimal.remote;

import dream.client.RemoteVar;
import dream.client.Signal;
import dream.examples.util.Client;

public class RemoteVars extends Client {
	private static final String HOSTNAME = "host2";

	public RemoteVars() {
		super(HOSTNAME);
		RemoteVar<Integer> rv = new RemoteVar<Integer>("host1", "counter");
		Signal<Integer> s = new Signal<Integer>("s", () -> {
			System.out.println(rv.get());
			return rv.get();
		}, rv);
	}
}
