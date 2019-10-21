package evalapp.generator;

import java.io.Serializable;

public class Counter implements Generator<Integer>, Serializable {

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
