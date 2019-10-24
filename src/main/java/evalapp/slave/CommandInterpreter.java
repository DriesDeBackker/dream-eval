package evalapp.slave;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import dream.client.DreamClient;
import dream.client.RemoteVar;
import dream.client.Signal;
import dream.client.UpdateProducer;
import dream.client.Var;
import dream.common.Consts;
import evalapp.commands.Command;
import evalapp.commands.IterationSpecifics;
import evalapp.commands.RemoteVarCommand;
import evalapp.commands.SignalCommand;
import evalapp.commands.VarCommand;
import evalapp.valgenerator.ValueGenerator;

public class CommandInterpreter {
	private String hostname;

	protected Logger logger;

	private Map<String, UpdateProducer<?>> reactivevars = new HashMap<String, UpdateProducer<?>>();

	private List<String> varsToWaitFor;

	public CommandInterpreter(String host) {
		this.hostname = host;
		Consts.hostName = host;

		logger = Logger.getLogger(host);

		DreamClient.instance.connect();

		addInitialVarsToWaitFor();

		waitForVars();

		RemoteVar<Command> cs = new RemoteVar<Command>("master", "commands");
		new Signal<Boolean>("processcommands", () -> {
			System.out.println("received command.");
			boolean deployed = process(cs.get());
			System.out.println(deployed);
			return deployed;
		}, cs);

		new Var<Boolean>("ready", Boolean.TRUE);

		System.out.println("Client initialization finished.");
		logger.fine("Client initialization finished.");
	}

	private void waitForVars() {
		// wait for vars needed by the client
		try {
			if (!this.varsToWaitFor.isEmpty())
				System.out.println("Waiting for Vars: " + this.varsToWaitFor.toString());
			logger.fine("Waiting for Vars: " + this.varsToWaitFor.toString());
			while (!allVarsAvailable()) {
				Thread.sleep(500);
			}
			assert (allVarsAvailable());
			System.out.println("Vars are now all available.");
			logger.fine("Vars are now all available.");
			this.varsToWaitFor.clear();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public boolean process(Command command) {
		if (command == null || !command.getTarget().equals(this.hostname)) {
			return false;
		}
		if (command instanceof VarCommand<?>) {
			System.out.println("Deploying a new Var.");
			logger.fine("Deploying a new Var.");
			this.processVarCommand((VarCommand<?>) command);
		} else if (command instanceof RemoteVarCommand) {
			System.out.println("Deploying a new RemoteVar.");
			logger.fine("Deploying a new RemoteVar.");
			this.processRemoteVarCommand((RemoteVarCommand) command);
		} else if (command instanceof SignalCommand) {
			System.out.println("Deploying a new Signal.");
			logger.fine("Deploying a new Signal.");
			this.processSignalCommand((SignalCommand) command);
		}
		return true;
	}

	private void processVarCommand(VarCommand<?> command) {
		// TODO add a thread that executes a modifying function enclosed in the
		// command!!!
		Var<?> newVar = new Var<>(command.getName(), command.getInitialValue());
		this.addReactiveVar(command.getName(), newVar);

		Thread t1 = new Thread(new Runnable() {
			public void run() {
				IterationSpecifics is = command.getIs();
				long mean = is.getMean();
				long sd = is.getSd();
				ValueGenerator<?> g = command.getGenerator();
				Random r = new Random();
				while (true) {
					newVar.setUnsafe(g.next());
					double next = r.nextGaussian() * sd + mean;
					try {
						Thread.sleep((long) next);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		t1.start();
	}

	private void processRemoteVarCommand(RemoteVarCommand command) {
		this.varsToWaitFor.add(command.getRemvar() + "@" + command.getRemhost());
		this.waitForVars();
		RemoteVar<? extends Serializable> newRemVar = new RemoteVar<>(command.getRemhost(), command.getRemvar());
		this.addReactiveVar(command.getRemvar(), newRemVar);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void processSignalCommand(SignalCommand command) {
		UpdateProducer<?>[] args = Arrays.stream(command.getArgs()).parallel().map(name -> this.reactivevars.get(name))
				.sequential().toArray(UpdateProducer<?>[]::new);
		Function<UpdateProducer<?>, ?> fn = command.getFn();
		int nbArgs = args.length;
		for (int i = 0; i < nbArgs - 1; i++) {
			fn = (Function<UpdateProducer<?>, ?>) fn.apply(args[i]);
		}
		final Function<UpdateProducer<?>, ?> fnend = fn;
		Supplier<?> closure = () -> fnend.apply(args[nbArgs - 1]);
		Signal<?> newSignal = new Signal<>(command.getName(), (Supplier<? extends Serializable>) closure, args);
		this.addReactiveVar(command.getName(), newSignal);
	}

	private void addInitialVarsToWaitFor() {
		this.varsToWaitFor = new ArrayList<String>();
		this.varsToWaitFor.add("commands@master");
	}

	private void addReactiveVar(String name, UpdateProducer<?> rv) {
		this.reactivevars.put(name, rv);
	}

	private boolean allVarsAvailable() {
		for (String var : this.varsToWaitFor) {
			if (!DreamClient.instance.listVariables().contains(var))
				return false;
		}
		return true;
	}

}
