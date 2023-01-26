package modules.json;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderTrakingResponse implements Serializable {


    @JsonProperty("numero_orden")
	public  Long numeroOrden;
    @JsonProperty("pickup_date")
	public  String pickupDate;
	@JsonProperty("nombre")
	public  String nombre;
	@JsonProperty("state")
	public  String state;
	@JsonProperty("mensaje_error")
	public String mensajeError;

    /**
     * @param pickupDate
     * @param nombre
     * @param state
     */
    @JsonCreator
    public OrderTrakingResponse(Long numeroOrden, Date pickupDate, String nombre, String state) {
        this.numeroOrden = numeroOrden;
        if (pickupDate != null) {
            String FULLDATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
            DateFormat FULLDATE_FORMATTER = new SimpleDateFormat(FULLDATE_PATTERN);
            this.pickupDate = FULLDATE_FORMATTER.format(pickupDate);
        } else
            this.pickupDate = null;
        this.nombre = nombre;
        this.state = state;
        this.mensajeError = "OK";
    }

    /**
     * @param mensajeError
     */
    public OrderTrakingResponse(Long numeroOrden, String mensajeError) {
        this.numeroOrden = numeroOrden;
        this.mensajeError = mensajeError;
        this.pickupDate = null;
        this.nombre = null;
        this.state = null;
    }

    @JsonIgnore
	public String toString() {
		StringBuffer sb = new StringBuffer();
		try {
            sb.append(JSonUtilities.getInstance().java2json(this));
        } catch (JsonProcessingException e) {
            sb.append("No se pudo convertir el objecto a String");
        }
		return sb.toString();
    }
    /**
     * @return the mensajeError
     */
    public String getMensajeError() {
        return mensajeError;
    }

    /**
     * @param mensajeError the mensajeError to set
     */
    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

}
