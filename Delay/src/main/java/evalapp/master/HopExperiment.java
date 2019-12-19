package evalapp.master;

import java.util.ArrayList;
import java.util.List;

import dream.client.RemoteVar;
import dream.client.Signal;
import dream.client.Var;

public class HopExperiment extends Client {
	private static final String HOSTNAME = "master";

	protected List<Long> delays;

	private Var<Long> sourceVar;

	public HopExperiment() {
		super(HOSTNAME);
	}

	@Override
	protected void init() {
		delays = new ArrayList<>();
		sourceVar = new Var<Long>("source", null);
	}

	@Override
	protected List<String> waitForVars() {
		List<String> varsToWaitFor = new ArrayList<String>();
		varsToWaitFor.add("hop@client");
		return varsToWaitFor;
	}

	public void run() {
		RemoteVar<Long> rv = new RemoteVar<>("client", "hop");

		new Signal<Boolean>("loghops", () -> {
			delays.add(System.currentTimeMillis() - rv.get());
			return Boolean.TRUE;
		}, rv);

		for (int i = 1; i < 200; i++) {
			sourceVar.set(System.currentTimeMillis());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println(this.delays.toString());

		Long sum = Long.valueOf(0);
		for (Long delay : delays) {
			sum += delay;
		}
		double mean = sum / delays.size();
		System.out.println("mean propagation delay: " + mean + "ms");
	}

}
