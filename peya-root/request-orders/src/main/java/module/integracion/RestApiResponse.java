package module.integracion;

import org.apache.commons.httpclient.Header;

public class RestApiResponse {
	private Header[] headers;
	private String body;
	private int statusCode;
	private String statusMessage;

	public RestApiResponse(String body) {
		this(body, new Header[0], 0, null);
	}
	public RestApiResponse(String body, Header[] headers, int statusCode, String statusMessage) {
		this.headers = headers;
		this.body = body;
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
	}
	public Header[] getHeaders() {
		return headers;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
}
