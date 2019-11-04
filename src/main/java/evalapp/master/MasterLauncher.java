package evalapp.master;

import evalapp.commands.Experiment;
import evalapp.master.experiment.DelayExperiment;
import evalapp.master.experiment.TrafficExperiment;

public class MasterLauncher {

	public static void main(String[] args) {
		System.out.println(Config.experiment);
		if (Config.experiment == Experiment.TRAFFIC) {
			System.out.println("Traffic, bitch");
			new TrafficExperiment().run();
		} else {
			System.out.println("Foooooooooooooooooooooooooooooooooooooooooooooooooooooooook");
			new DelayExperiment().run();
		}
	}

}
