package oncue;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public class RunAgentValidator implements IValueValidator<String> {

	@Override
	public void validate(String name, String value) throws ParameterException {
		throw new ParameterException("Unimplemented!");
	}

}
