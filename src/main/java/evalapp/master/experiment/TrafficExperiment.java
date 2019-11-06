package evalapp.master.experiment;

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
		// We do not actively stop the experiment as this affects the average
		// traffic rate in e.g. Wireshark if the latter is still measuring.
	}
}
