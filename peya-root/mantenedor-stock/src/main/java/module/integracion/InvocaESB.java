package module.integracion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;

import cl.ahumada.esb.dto.pedidosYa.consultaStock.MantencionStockResponse;
import cl.ahumada.esb.dto.pedidosYa.consultaStock.json.Items;
import cl.ahumada.esb.dto.pharol.consultastock.ConsultaStockRequest;
import cl.ahumada.esb.dto.pharol.consultastock.ConsultaStockResponse;
import cl.ahumada.esb.dto.pharol.json.Stock;
import cl.ahumada.esb.dto.pharolV4.pedidos.PedidosRequest;
import cl.ahumada.esb.utils.json.JSonUtilities;

public class InvocaESB extends RestApiClient {

	protected final String stockEndPoint= "http://localhost:8080/ESB/Stock/tienda/buscar";
	protected final String pedidosEndPoint = "http://localhost:8080/ESB/PedidosV4/detalle/generar";
	protected Properties integracionProps;
	protected Integer stockCritico = 2;
	protected String endpointMantieneStock;
	protected String endpointInformeDeshabilita = "http://localhost:8080/ESB/informeDeshabilitado/skusOrder";

	public InvocaESB(Properties integracionProps) throws IOException {
		super();
		this.integracionProps = integracionProps;
		String valor = integracionProps.getProperty(ServiciosdeBus.STOCK_CRITICO_KEY);
		if (valor != null) {
			try {
				stockCritico = Integer.valueOf(valor);
			} catch (Exception e) {
				String msg = String.format("Propiedad %s tiene valor %s no numerico",ServiciosdeBus.STOCK_CRITICO_KEY, valor);
				logger.error(msg, e);
				throw new RuntimeException(msg);
			}
		}
		try (final java.io.InputStream stream =
           this.getClass().getClassLoader().getResourceAsStream("authorizationProps.properties")){
			if (stream == null) throw new RuntimeException("stream es nulo");
		authorizationProps.load(stream);
		}
		httpclient = new HttpClient();
		endpointMantieneStock = integracionProps.getProperty(ServiciosdeBus.ENDPOINT_MANTIENE_STOCK_KEY);
		endpointInformeDeshabilita = integracionProps.getProperty(ServiciosdeBus.ENDPOINT_INFORMA_DESHABILITA_KEY);
	}

	/**
	 * @param map
	 * @return
	 */
	public Boolean hayStock(Map<String,Object> map) {
		return hayStock(map, null);
	}

	public Boolean hayStockConReserva(Map<String,Object> map) {
		return hayStock(map, stockCritico);
	}
	public Boolean hayStock(Map<String,Object> map, Integer critico) {
		ConsultaStockRequest request = (ConsultaStockRequest) map.get(ServiciosdeBus.REQUEST_STOCK_KEY);

		// true si hay stock para todos los productos
		try {
			ConsultaStockResponse stockResponse = (ConsultaStockResponse)invocaEsbServiceStock(request);
			if (stockResponse != null) {
				map.put("StockResponse", stockResponse);
				return hayStock(request.local[0].stock, stockResponse.local[0].stock, map, stockCritico);
			}
			throw new RuntimeException("No pudo comunicarse con el ESB");

		} catch (Exception e) {
			String json = "No pudo convertir";
			try {
				json = JSonUtilities.getInstance().java2json(request);
			} catch (JsonProcessingException e1) {
				;
			}
			logger.error(String.format("hayStock: local:\n%s",json), e);
		}

		return null;
	}

	public Integer getStockCritico() {
		return stockCritico;
	}

	@SuppressWarnings("unchecked")
	private Boolean hayStock(Stock[] stockPedido, Stock[] stockDisponible, Map<String,Object> map, Integer stockCritico) {
		// true si hay suficiente stock para todo lo pedido
		// coloca en el map, la lista de productos que no tienen stock
		boolean hayStock = true;
		for (Stock pedido : stockPedido) {
			long cpPedido = pedido.codigoProducto;
			boolean cumple;
			if (stockCritico == null)
				cumple =suficienteStock(cpPedido, pedido.cantidad, stockDisponible);
			else
				cumple = suficienteStock(cpPedido, pedido.cantidad, stockDisponible, stockCritico);

			if (!cumple) {
				hayStock = false;
				List<Long> productosSinStock = (List<Long>) map.get(ServiciosdeBus.PRODUCTOS_SIN_STOCK_KEY);
				if (productosSinStock == null) {
					productosSinStock = new ArrayList<Long>();
					map.put(ServiciosdeBus.PRODUCTOS_SIN_STOCK_KEY, productosSinStock);
				}
				productosSinStock.add(Long.valueOf(cpPedido));
			}
		}
		return hayStock;
	}

	private boolean suficienteStock(long cpPedido, long cantidadPedida, Stock[] stockDisponible) {
		// true si cantidad pedida <= stock
		for (Stock disponible : stockDisponible) {
			if (cpPedido != disponible.codigoProducto)
				continue;
			if (cantidadPedida <= disponible.cantidad)
				return true;
		}
		return false;
	}

	private boolean suficienteStock(long cpPedido, long cantidadPedida, Stock[] stockDisponible, Integer critico) {
		// true si cantidad pedida <= stock
		for (Stock disponible : stockDisponible) {
			if (cpPedido != disponible.codigoProducto)
				continue;
			if (cantidadPedida <= (disponible.cantidad - critico))
				return true;
		}
		return false;
	}

	/**
	 * @param map
	 * @return
	 */
	public boolean realizarPedido(Map<String,Object> map) {
		PedidosRequest request = (PedidosRequest) map.get(ServiciosdeBus.REQUEST_PEDIDOS_KEY);
		try {
			String json = JSonUtilities.getInstance().java2gson(request);

			RestApiResponse ar = invokeEndpoint(pedidosEndPoint, json);

			int httpStatus = ar.getStatusCode();
			if (httpStatus < 300) {
				String jsonresp = ar.getBody();
				logger.debug(String.format("invocaEsbServiceStock: del esb pedido aceptado: %s", jsonresp));
				return true;
			} else {
				map.put(ServiciosdeBus.RESULTADO_OPERACION_KEY, String.format("%d", httpStatus));
			}
		} catch (Exception e) {
			logger.error("", e);
			map.put(ServiciosdeBus.RESULTADO_OPERACION_KEY, String.format("error: %s", e.getMessage()));
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private ConsultaStockResponse invocaEsbServiceStock(ConsultaStockRequest request) {
		try {
			String json = JSonUtilities.getInstance().java2json(request);

			RestApiResponse ar = invokeEndpoint(stockEndPoint, json);

			int httpStatus = ar.getStatusCode();
			if (httpStatus < 300) {
				String jsonresp = String.format("{\"ConsultaStockResponse\":%s}",ar.getBody());
				logger.debug(String.format("invocaEsbServiceStock: del esb: %s", jsonresp));
				return (ConsultaStockResponse) JSonUtilities.getInstance().json2java(jsonresp, ConsultaStockResponse.class);
			}
		} catch (Exception e) {
			logger.error("invocaEsbServiceStock", e);
		}
		return null;
	}


	public Items[] invocaWsMantieneStock(Integer funcion) {
		String url = String.format("%s/%d", endpointMantieneStock, funcion);
		try {
			RestApiResponse ar = invokeEndpoint(url, null);
			int httpStatus = ar.getStatusCode();
			if (httpStatus < 300) {
				String jsonresp = String.format("{\"MantencionStockResponse\":%s}",ar.getBody());
				logger.debug(String.format("invocaWsMantieneStock: del esb: %s", jsonresp));
				@SuppressWarnings("deprecation")
				MantencionStockResponse msr = (MantencionStockResponse) JSonUtilities.getInstance().json2java(jsonresp, MantencionStockResponse.class);
				return msr.getItems();
			}
		} catch (Exception e) {
			logger.error("invocaWsMantieneStock", e);
		}
		return null;
	}

}
