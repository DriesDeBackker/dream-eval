package evalapp.master;

public class MasterLauncher {

	public static void main(String[] args) {
		System.out.println("Experiment type: HOP");
		new HopExperiment().run();
	}

}
