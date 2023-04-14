package module.integracion.debug;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DProduct implements Serializable {

	/**
	 * 
	 */
	@JsonIgnore
	private static final long serialVersionUID = -3153489127749348538L;
	@JsonProperty("integrationCode")
	private String integrationCode;
	@JsonProperty("sectionName")
	private String sectionName;
	@JsonProperty("id")
	private Long id;
	
	@JsonCreator
	public DProduct(
			@JsonProperty("integrationCode")String integrationCode, 
			@JsonProperty("sectionName")String sectionName, 
			@JsonProperty("id")Long id) {
		super();
		this.integrationCode = integrationCode;
		this.sectionName = sectionName;
		this.id = id;
	}

	public String getIntegrationCode() {
		return integrationCode;
	}

	public void setIntegrationCode(String integrationCode) {
		this.integrationCode = integrationCode;
	}

	public String getSectionName() {
		return sectionName;
	}

	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
		
}
