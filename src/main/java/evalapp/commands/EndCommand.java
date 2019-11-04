package evalapp.commands;

public class EndCommand extends Command {

	private static final long serialVersionUID = 2840335575242027000L;
	private Experiment experiment;

	public EndCommand(String target, Experiment exp) {
		super(target);
		this.setExperiment(exp);
	}

	public Experiment getExperiment() {
		return experiment;
	}

	private void setExperiment(Experiment experiment) {
		this.experiment = experiment;
	}

}
