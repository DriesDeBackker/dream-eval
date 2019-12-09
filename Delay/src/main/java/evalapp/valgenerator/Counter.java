package evalapp.valgenerator;

import java.io.Serializable;

import evalapp.valgenerator.ValueGenerator;

public class Counter implements ValueGenerator<Integer>, Serializable {

	private static final long serialVersionUID = -2596743613103381156L;
	Integer current;

	public Counter(Integer initial) {
		this.current = initial;
	}

	@Override
	public Integer next() {
		current++;
		return current;
	}

}
