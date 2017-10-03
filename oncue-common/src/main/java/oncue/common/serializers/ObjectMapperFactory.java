package oncue.common.serializers;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class ObjectMapperFactory {

	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
		mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
		mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
		mapper.registerModule(new JodaModule());
	}

	private ObjectMapperFactory() {
	}

	public static ObjectMapper getInstance() {
		return mapper;
	}
}