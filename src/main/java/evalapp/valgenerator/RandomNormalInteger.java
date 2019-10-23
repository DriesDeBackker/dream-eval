package evalapp.valgenerator;

import java.io.Serializable;
import java.util.Random;

import evalapp.valgenerator.ValueGenerator;

public class RandomNormalInteger implements ValueGenerator<Integer>, Serializable {

	private static final long serialVersionUID = -9056726378165269760L;
	private final long mean;
	private final long sd;
	private final Random r;

	public RandomNormalInteger(long mean, long sd) {
		this.mean = mean;
		this.sd = sd;
		this.r = new Random();
	}

	@Override
	public Integer next() {
		// TODO Auto-generated method stub
		return (int) (r.nextGaussian() * sd + mean);
	}

}
