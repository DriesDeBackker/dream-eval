package evalapp.master;

import evalapp.commands.Experiment;
import evalapp.master.experiment.DelayExperiment;
import evalapp.master.experiment.TrafficExperiment;

public class MasterLauncher {

	public static void main(String[] args) {
		System.out.println("Experiment type: " + Config.experiment);
		if (Config.experiment == Experiment.TRAFFIC) {
			new TrafficExperiment().run();
		} else {
			new DelayExperiment().run();
		}
	}

}
