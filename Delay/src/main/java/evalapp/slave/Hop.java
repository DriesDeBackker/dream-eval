package evalapp.slave;

import java.util.ArrayList;
import java.util.List;

import dream.client.DreamClient;
import dream.client.RemoteVar;
import dream.client.Signal;
import dream.common.Consts;

public class Hop {
	private List<String> varsToWaitFor;

	public Hop(String host) {
		Consts.hostName = host;

		DreamClient.instance.connect();

		this.varsToWaitFor = new ArrayList<String>();
		this.varsToWaitFor.add("source@master");

		waitForVars();

		RemoteVar<Long> hop = new RemoteVar<>("master", "source");

		new Signal<Long>("hop", () -> {
			return hop.get();
		}, hop);

		System.out.println("Client initialization finished.");

		this.start();
	}

	private void start() {
		System.out.println("Experiment started");
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void waitForVars() {
		// wait for vars needed by the client
		try {
			if (!this.varsToWaitFor.isEmpty())
				System.out.println("Waiting for Vars: " + this.varsToWaitFor.toString());
			while (!allVarsAvailable()) {
				Thread.sleep(500);
			}
			assert (allVarsAvailable());
			System.out.println("Vars are now all available.");
			this.varsToWaitFor.clear();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean allVarsAvailable() {
		for (String var : this.varsToWaitFor) {
			if (!DreamClient.instance.listVariables().contains(var))
				return false;
		}
		return true;
	}

}
