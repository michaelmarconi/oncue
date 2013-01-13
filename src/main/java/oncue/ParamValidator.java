package oncue;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ParamValidator implements IParameterValidator {

	@Override
	public void validate(String name, String value) throws ParameterException {
		if (!name.equals("-param"))
			throw new ParameterException("Can only use a ParamValidator on -param parameters!");

		String[] parts = value.split("=");
		if (parts.length != 2)
			throw new ParameterException("Each parameter is a key/value pair split by an '=' character");
	}

}
