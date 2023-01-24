package module.integracion;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.ssl.Base64;
import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;

public class RestApiClient {

	protected Properties httpClientProps = new Properties();
	protected Properties postHeaderProps = new Properties();
	protected final Properties authorizationProps = new Properties();
	protected HttpClient httpclient;

	protected Logger logger = Logger.getLogger(getClass());

	public RestApiResponse invokeEndpoint(String endpoint, String request) throws Exception {
        String contentType = "application/json;charset=UTF-8";
		logger.info(String.format("invokeEndpoint: endpoint=%s\njson:%s",endpoint,request));
        PostMethod post = new PostMethod(endpoint);

        post.setRequestHeader("Content-Type", contentType);
        post.setRequestHeader("Connection", "keep-alive");

        if (!postHeaderProps.isEmpty()) {
        	Enumeration<Object> en = postHeaderProps.keys();
        	while (en.hasMoreElements()) {
        		String headerName = (String)en.nextElement();
        		String headerValue = postHeaderProps.getProperty(headerName);
        		post.setRequestHeader(headerName, headerValue);
        	}
        }
        if (!authorizationProps.isEmpty()) {
        	String type = authorizationProps.getProperty("type");
        	String authorizationHeaderValue = null;
        	if (type != null && "Bearer".equalsIgnoreCase(type)) {
        		String token = authorizationProps.getProperty("tokenKey"); // DEBE proveerla el padre
        		authorizationHeaderValue = String.format("%s %s", type, token);
	    		logger.debug(String.format("invokeEndpoint: autorizacion %s", authorizationHeaderValue));
        	} else {
        		type = "Basic";
	        	String username = authorizationProps.getProperty("username");
	        	String password = authorizationProps.getProperty("password");
	        	if (username != null && password != null) {
		        	String usernameAndPassword = username + ":" + password;
		        	byte[] encodedAuth = Base64.encodeBase64(usernameAndPassword.getBytes(Charset.forName("US-ASCII")));
		        	authorizationHeaderValue = String.format("%s %s", type, new String(encodedAuth));
		    		logger.debug(String.format("invokeEndpoint: autorizacion %s %s %s", username,password,authorizationHeaderValue));
		        }
        	}
        	post.setRequestHeader(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
    		post.setDoAuthentication(true);
        }
        try {
        	if (request != null && request.length() > 0)
        		post.setRequestEntity(new StringRequestEntity(request, "application/json", "UTF-8"));

        	logger.debug("invokeEndpoint: antes del POST");
            int result = httpclient.executeMethod(post);
    		logger.debug(String.format("invokeEndpoint: despues del POST; result=%d",result));
            if(result != HttpStatus.SC_OK) {
                logger.warn("Received status code '" + result + "' on HTTP (POST) request to '" + endpoint + "'.");
                if (result == HttpStatus.SC_BAD_REQUEST) {
                    return new RestApiResponse(post.getResponseBodyAsString(), post.getResponseHeaders(), result, post.getStatusLine().getReasonPhrase());
                }
            }
            String rBody = post.getResponseBodyAsString();
            String rsl = post.getStatusLine()!= null ? post.getStatusLine().getReasonPhrase() : "";

            logger.debug(String.format("invokeEndpoint: JSON Respuesta invocacion a %s: result:%d statusLine: %s\n%s",
            		                    endpoint, result, rsl, rBody));

            return new RestApiResponse(rBody, post.getResponseHeaders(), result, rsl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke Endpoint: '" + endpoint  + "'.", e);
        } finally {
            post.releaseConnection();
        }
    }

}
