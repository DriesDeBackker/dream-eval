package evalapp.valgenerator;

import java.io.Serializable;

public interface ValueGenerator<T extends Serializable> {

	public T next();

}
