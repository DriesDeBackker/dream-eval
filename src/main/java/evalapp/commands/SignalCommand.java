package evalapp.commands;

import java.io.Serializable;
import java.util.function.Function;

import dream.client.UpdateProducer;

public class SignalCommand extends Command implements Serializable {

	private static final long serialVersionUID = -5516875372186775573L;

	private String[] args;
	private Function<UpdateProducer<?>, ?> fn;
	private String name;

	public SignalCommand(String target, String name, Function<UpdateProducer<?>, ?> fn, String... args) {
		super(target);
		this.setName(name);
		this.setFn(fn);
		this.setArgs(args);
	}

	/*
	 * public SignalCommand(String target, String name,
	 * Function<UpdateProducer<?>, Function<UpdateProducer<?>, ?>> fn, String
	 * arg1, String arg2) { super(target); this.fn = fn; }
	 * 
	 * public SignalCommand(String target, String name,
	 * Function<UpdateProducer<?>, Function<UpdateProducer<?>,
	 * Function<UpdateProducer<?>, ?>>> fn, String arg1, String arg2, String
	 * arg3) { super(target); this.fn = fn; }
	 */

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public Function<UpdateProducer<?>, ?> getFn() {
		return fn;
	}

	private void setFn(Function<UpdateProducer<?>, ?> fn) {
		this.fn = fn;
	}

	public String[] getArgs() {
		return args;
	}

	private void setArgs(String[] args) {
		this.args = args;
	}

}
