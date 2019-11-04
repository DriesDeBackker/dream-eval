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
		// TODO Auto-generated method stub

	}

	@Override
	protected void gatherResults() {
		// Nothing to do here: results gathered using e.g. Wireshark.
	}

	@Override
	protected void processResults() {
		// Nothing to do here.
	}
}
