package evalapp.generator;

import java.io.Serializable;

public interface Generator<T extends Serializable> {

	public T next();

}
