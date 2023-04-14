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

import com.pedidosya.reception.sdk.models.Product;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DItem implements Serializable {

	/**
	 * 
	 */
	@JsonIgnore
	private static final long serialVersionUID = 545507588335594155L;
	@JsonProperty("vendorId")
	private Long vendorId;
	@JsonProperty("productos")
	private DProduct[] productos;

	public DItem(Map<Long, Product> map) {
		Set<Long> keys = map.keySet();
		List<DProduct> lista = new ArrayList<DProduct>();
		for (Long key : keys) {
			setVendorId(key);
			Product prod = map.get(key);
			lista.add(new DProduct(prod.getIntegrationCode(), prod.getSection().getName(), prod.getId()));
		}
		this.setProductos(lista.toArray(new DProduct[0]));
	}

	public Long getVendorId() {
		return vendorId;
	}

	public void setVendorId(Long vendorId) {
		this.vendorId = vendorId;
	}

	public DProduct[] getProductos() {
		return productos;
	}

	public void setProductos(DProduct[] productos) {
		this.productos = productos;
	}
}
