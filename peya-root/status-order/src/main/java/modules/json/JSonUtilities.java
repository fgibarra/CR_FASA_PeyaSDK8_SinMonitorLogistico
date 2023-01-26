package modules.json;

import java.io.Reader;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSonUtilities {
	private static JSonUtilities instance;
	protected Logger logger = Logger.getLogger(getClass());

	public static JSonUtilities getInstance() {
		if (instance == null) instance = new JSonUtilities();
		return instance;
	}

	public JSonUtilities() {
		super();
	}

	// ------------------------------------------------------
	// con JACKSON
	// ------------------------------------------------------
	public Object json2java(String jsonString, Class<?> response) throws Exception {
		//logger.debug(jsonString.substring(0, jsonString.length()>500?500:jsonString.length()));
		logger.debug("convertir a Class ="+response);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);

		try {
			return mapper.readValue(jsonString, response);
		} catch (Exception e) {
			logger.error("json2java: json\n"+jsonString);
			throw e;
		}
	}

	public Object json2java(String jsonString, Class<?> response, boolean UNWRAP_ROOT_VALUE) throws Exception {
		//logger.debug(jsonString.substring(0, jsonString.length()>500?500:jsonString.length()));
		logger.debug("convertir a Class ="+response);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, UNWRAP_ROOT_VALUE);

		try {
			return mapper.readValue(jsonString, response);
		} catch (Exception e) {
			logger.error(String.format("json2java:ERROR al convertir a clase %s de json\n%s",response,jsonString), e);
			throw e;
		}
	}

	public Object json2java(Reader rd, Class<?>response) throws Exception {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(rd, response);
		} catch (Exception e) {
			logger.error(String.format("json2java:ERROR al convertir a clase %s de json\n%s",response), e);
			throw e;
		}
	}

	public String java2json (Object data) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		return mapper.writeValueAsString(data);
	}
    
}
