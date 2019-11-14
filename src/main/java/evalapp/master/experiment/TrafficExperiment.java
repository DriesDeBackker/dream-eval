package evalapp.master.experiment;

import java.util.ArrayList;
import java.util.List;

import dream.client.DreamClient;
import evalapp.commands.Experiment;

public class TrafficExperiment extends ProgramDeployer {
	private static final String HOSTNAME = "master";
	private static Experiment EXPERIMENT = Experiment.TRAFFIC;

	public TrafficExperiment() {
		super(HOSTNAME, EXPERIMENT);
	}

	@Override
	protected void prepareExperiment() {
		// Nothing to do here.
	}

	@Override
	protected void endExperiment() {
		System.out.println("TIME'S UP! EXPERIMENT FINISHED!");
		this.runningVar.set(Boolean.FALSE);

		List<String> finishedVars = new ArrayList<>();
		for (int i = 0; i < clients.length; i++) {
			finishedVars.add("finished@" + clients[i]);
		}
		while (!finished(finishedVars)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("OK: RESULTS VALID");
	}

	private boolean finished(List<String> finishedVars) {
		for (String var : finishedVars) {
			if (!DreamClient.instance.listVariables().contains(var)) {
				return false;
			} else {
				System.out.println("Client" + Integer.toString(finishedVars.indexOf(var) + 1) + " finished");
			}
		}
		return true;
	}
}
