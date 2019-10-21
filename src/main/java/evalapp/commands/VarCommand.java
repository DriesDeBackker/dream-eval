package evalapp.commands;

import java.io.Serializable;

import evalapp.generator.Generator;

public class VarCommand<T extends Serializable> extends Command implements Serializable {

	private static final long serialVersionUID = 8886245125293165876L;
	private String name;
	private T initialValue;
	private Generator<T> generator;
	private IterationSpecifics is;

	public VarCommand(String target, String name, T initialValue, Generator<T> generator, IterationSpecifics is) {
		super(target);
		this.setName(name);
		this.setInitialValue(initialValue);
		this.setGenerator(generator);
		this.setIs(is);
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public T getInitialValue() {
		return initialValue;
	}

	private void setInitialValue(T initialValue) {
		this.initialValue = initialValue;
	}

	public Generator<T> getGenerator() {
		return this.generator;
	}

	private void setGenerator(Generator<T> generator) {
		this.generator = generator;
	}

	public IterationSpecifics getIs() {
		return is;
	}

	private void setIs(IterationSpecifics is) {
		this.is = is;
	}

}
