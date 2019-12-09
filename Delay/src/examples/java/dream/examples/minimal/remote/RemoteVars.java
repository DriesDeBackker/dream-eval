package dream.examples.minimal.remote;

import dream.client.RemoteVar;
import dream.client.Signal;
import dream.examples.util.Client;

public class RemoteVars extends Client {
	private static final String HOSTNAME = "host2";

	public RemoteVars() {
		super(HOSTNAME);
		RemoteVar<Integer> rv1 = new RemoteVar<Integer>("host1", "var1");
		RemoteVar<Integer> rv2 = new RemoteVar<Integer>("host1", "var2");
		new Signal<>("s", () -> {
			Integer val1 = rv1.get();
			Integer val2 = rv2.get();
			Integer newval;
			if (val1 == null) {
				newval = val2;
				System.out.println("one of the values is null");
			} else if (val2 == null) {
				newval = val1;
				System.out.println("one of the values is null");
			} else {
				newval = Math.round((val1 + val2) / 2);
			}
			System.out.println(newval);
			return newval;
		}, rv1, rv2);
	}
}
