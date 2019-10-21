package evalapp.master;

import java.io.Serializable;
import java.util.function.Function;

import dream.client.UpdateProducer;
import dream.client.Var;
import dream.examples.util.Client;
import evalapp.commands.Command;
import evalapp.commands.IterationSpecifics;
import evalapp.commands.RemoteVarCommand;
import evalapp.commands.SignalCommand;
import evalapp.commands.VarCommand;
import evalapp.generator.Generator;
import evalapp.generator.RandomNormalInteger;

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

		Generator<Integer> g = new RandomNormalInteger(1, 100);
		IterationSpecifics is = new IterationSpecifics(3000, 100);
		Command c2 = new VarCommand<Integer>("slave1", "randint", 10, g, is);
		cmds.set(c2);

		Function<UpdateProducer<?>, ?> fn = (Function<UpdateProducer<?>, Function<UpdateProducer<?>, ?>> & Serializable) nvar -> mvar -> {
			Integer x = (Integer) nvar.get() + (Integer) mvar.get();
			System.out.println(x);
			return x;
		};
		Command c3 = new SignalCommand("slave1", "printer", fn, "counter", "randint");
		cmds.set(c3);
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
