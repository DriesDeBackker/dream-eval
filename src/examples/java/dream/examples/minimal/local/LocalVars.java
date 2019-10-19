package dream.examples.minimal.local;

import dream.client.Var;
import dream.examples.util.Client;

public class LocalVars extends Client {
	private static final String HOSTNAME = "host1";

	public LocalVars() {
		super(HOSTNAME);
		Var<Integer> a = new Var<>("counter", Integer.valueOf(0));
		count(a);
	}

	private void count(Var<Integer> a) {
		Integer counter = 1;
		while (true) {
			a.set(counter);
			counter++;
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
