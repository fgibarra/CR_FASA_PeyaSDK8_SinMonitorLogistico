package module.integracion.debug;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pedidosya.reception.sdk.models.Product;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DumpClass implements Serializable {

	/**
	 * 
	 */
	@JsonIgnore
	private static final long serialVersionUID = 2763103413735399826L;
	@JsonProperty("items")
	private DItem[] items;
	
	public DumpClass (Map<Long, Map<Long, Product>> mapdeMaps) {
		Set<Long> keys = mapdeMaps.keySet();
		List<DItem> listaItems = new ArrayList<DItem>();
		for (Long key : keys) {
			listaItems.add(new DItem(mapdeMaps.get(key)));
		}
		this.setItems(listaItems.toArray(new DItem[0]));
	}

	public DumpClass() {
		super();
	}

	@Override
	@JsonIgnore
	public String toString() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			return mapper.writeValueAsString(this);
		} catch (Exception e) {
			return String.format("No pudo serializar %s",this.getClass().getSimpleName());
		}
	}

	public DItem[] getItems() {
		return items;
	}

	public void setItems(DItem[] items) {
		this.items = items;
	}

}
