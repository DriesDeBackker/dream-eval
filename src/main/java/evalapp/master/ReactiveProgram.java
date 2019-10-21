package evalapp.master;

import java.io.Serializable;
import java.util.function.Function;

import dream.client.UpdateProducer;
import dream.client.Var;
import dream.examples.util.Client;
import evalapp.commands.Command;
import evalapp.commands.RemoteVarCommand;
import evalapp.commands.SignalCommand;

public class ReactiveProgram extends Client {
	private static final String HOSTNAME = "master";

	public ReactiveProgram() {
		super(HOSTNAME);
		Var<Command> cmds = new Var<Command>("commands", null);

		// Local var for testing purposes only (Remove!!)
		Var<Integer> test = new Var<>("counter", Integer.valueOf(0));

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		deployProgram(cmds);

		count(test);
	}

	@SuppressWarnings("unchecked")
	private void deployProgram(Var<Command> cmds) {
		System.out.println("Deploying distributed reactive program");
		Command c1 = new RemoteVarCommand("slave1", "master", "counter");
		cmds.set(c1);
		Function<UpdateProducer<?>, ?> fn = (Function<UpdateProducer<?>, ?> & Serializable) nvar -> {
			Integer n = (Integer) nvar.get();
			System.out.println(n);
			return n;
		};
		Command c2 = new SignalCommand("slave1", "printer", fn, "counter");
		cmds.set(c2);
	}

	private void count(Var<Integer> test) {
		Integer counter = 1;
		while (true) {
			test.set(counter);
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
