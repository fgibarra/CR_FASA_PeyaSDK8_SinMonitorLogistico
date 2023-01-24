package module.integracion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pedidosya.reception.sdk.models.Order;

import cl.ahumada.esb.dto.pedidosYa.consultaStock.MantencionStockResponse;
import cl.ahumada.esb.dto.pedidosYa.consultaStock.json.Items;
import cl.ahumada.esb.dto.pharol.consultastock.ConsultaStockRequest;
import cl.ahumada.esb.dto.pharol.consultastock.ConsultaStockResponse;
import cl.ahumada.esb.dto.pharol.json.Local;
import cl.ahumada.esb.dto.pharol.json.Stock;
import cl.ahumada.esb.dto.pharolV4.pedidos.PedidosRequest;
import cl.ahumada.esb.utils.json.JSonUtilities;
import module.Requester;

public class InvocaESB extends RestApiClient {

	protected String stockEndPoint="http://150.10.255.93:8181/cxf/ESB/Stock/tienda/buscar";
	protected String stockEndPoint2="http://localhost:8080/ESB/Stock/tienda/buscar";
	protected String pedidosEndPoint="http://localhost:8080/ESB/PedidosV4/detalle/generar";
	protected String endpointMantieneStock="http://localhost:8080/ESB/mantieneStock/tienda/buscar";
	protected String endpointInformeDeshabilita="http://localhost:8080/ESB/informeDeshabilitado/skusOrder";
	protected Properties integracionProps;
	protected Integer stockCritico = 2;

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
		stockEndPoint = integracionProps.getProperty(ServiciosdeBus.ENDPOINT_STOCK_KEY);
		stockEndPoint2 = integracionProps.getProperty(ServiciosdeBus.ENDPOINT_STOCK2_KEY);
		endpointMantieneStock = integracionProps.getProperty(ServiciosdeBus.ENDPOINT_MANTIENE_STOCK_KEY);
		endpointInformeDeshabilita = integracionProps.getProperty(ServiciosdeBus.ENDPOINT_INFORMA_DESHABILITA_KEY);
		logger.info(String.format("endpoint_ws_mantiene_stock=%s "+
				"endpoint_ws_informeDeshabilita=%s stockEndPoint=%s pedidosEndPoint=%s",
				integracionProps.get(ServiciosdeBus.ENDPOINT_MANTIENE_STOCK_KEY),
				integracionProps.get(ServiciosdeBus.ENDPOINT_INFORMA_DESHABILITA_KEY),
				integracionProps.get("stockEndPoint"),
				integracionProps.get("pedidosEndPoint")));
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
			logger.debug(String.format("hayStock: stockResponse: %s",
					stockResponse!=null?String.format("vienen datos:%s", stockResponse) :"NULO"));
			if (stockResponse != null && stockResponse.local != null) {
				map.put("StockResponse", stockResponse);
				return hayStock(request.local[0].stock, stockResponse.local[0].stock, map, stockCritico);
			}
			throw new RuntimeException((stockResponse != null && stockResponse.local == null)?
					"viene un response vacio {} sin local":"No pudo comunicarse con el ESB");

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
			int retryCount = 2;
			do {
				RestApiResponse ar = invokeEndpoint(pedidosEndPoint, json);
	
				int httpStatus = ar.getStatusCode();
				if (httpStatus < 300) {
					String jsonresp = ar.getBody();
					logger.debug(String.format("invocaEsbServiceStock.realizarPedido: del esb pedido aceptado: %s", jsonresp));
					return true;
				} else if (httpStatus == 500) {
					retryCount--;
					if (retryCount > 0)
						logger.info(String.format("invocaEsbServiceStock.realizarPedido: se recibio error 500: (%s) reintento %d",
								ar.getStatusMessage(), retryCount));
				} else {
					map.put(ServiciosdeBus.RESULTADO_OPERACION_KEY, String.format("%d", httpStatus));
				}
			} while (retryCount > 0);
		} catch (Exception e) {
			logger.error("", e);
			map.put(ServiciosdeBus.RESULTADO_OPERACION_KEY, String.format("error: %s", e.getMessage()));
		}
		return false;
	}

	private ConsultaStockResponse invocaEsbServiceStock(ConsultaStockRequest request) {
		ConsultaStockResponse r1 = invocaEsbServiceStock(request, stockEndPoint);
		if (r1 != null)
			return r1;
		r1 = invocaEsbServiceStock(request, stockEndPoint2);
		if (r1 != null)
			return r1;

		return  new ConsultaStockResponse( new Local[] {new Local(request.local[0].numeroLocal, new Stock[] {new Stock(request.local[0].numeroLocal, Long.valueOf(stockCritico+1))})});
	}

	private ConsultaStockResponse invocaEsbServiceStock(ConsultaStockRequest request, String endpoint) {
		logger.info(String.format("invocaEsbServiceStock: consulta stock a servidor: %s", endpoint));
		try {
			String json = JSonUtilities.getInstance().java2json(request);

			RestApiResponse ar = invokeEndpoint(endpoint, json);
			logger.debug(String.format("invocaEsbServiceStock: stockEndPoint responde: %s",
					ar!=null?String.format("ar.status=%d", ar.getStatusCode()):"NULO"));
			int httpStatus = ar.getStatusCode();
			if (httpStatus < 300) {
				String jsonresp = String.format("{\"ConsultaStockResponse\":%s}",ar.getBody());
				logger.debug(String.format("invocaEsbServiceStock: del esb: %s", jsonresp));
				return (ConsultaStockResponse) JSonUtilities.getInstance().json2java(jsonresp, ConsultaStockResponse.class);
				//return (ConsultaStockResponse) JSonUtilities.getInstance().gson2java(jsonresp, ConsultaStockResponse.class);
			}
		} catch (Exception e) {
			logger.error("invocaEsbServiceStock", e);
		}
		return null;
	}


	public void InformeRechazo(Map<String, Object> map) {
		Order order = (Order) map.get("order");
		ConsultaStockResponse stockResponse = (ConsultaStockResponse)map.get("StockResponse");
		//ConsultaStockRequest requestPedidos = (ConsultaStockRequest)map.get(ServiciosdeBus.REQUEST_STOCK_KEY);

		Long orderId = order.getId();
		String local = order.getRestaurant().getIntegrationCode();
		@SuppressWarnings("unchecked")
		List<Long> productosSinStock = (List<Long>) map.get(ServiciosdeBus.PRODUCTOS_SIN_STOCK_KEY);

		StringBuffer sb = new StringBuffer();
		for (Long sku: productosSinStock) {
			sb.append(String.format("%d", sku)).append('-');
			Long cantidadEnStock = Requester.buscaCantidadEnStock(sku, stockResponse);

			Stock stock = null;
			for (Stock pedido : stockResponse.local[0].stock) {
				long cpPedido = pedido.codigoProducto;
				if (sku == cpPedido) {
					stock = pedido;
					break;
				}
			}
			if (stock != null && cantidadEnStock != null)
				sb.append(stock.cantidad).append('-').append(getCantidadSolicitada(sku, map)).append('-');
			else {
				logger.error(String.format("Encuentra Stock en nulo cuando procesa respuesta: %s",
						stockResponse.toString()));
			}
		}
		logger.debug(String.format("informaPedidoRechazado: orderId %d local %s skus: %s",
				orderId, local, sb.toString()));

		String url = String.format("%s/%d/%s/%s", endpointInformeDeshabilita,
				orderId, local, sb.toString());

		try {
			RestApiResponse ar = invokeEndpoint(url, null);
			int httpStatus = ar.getStatusCode();
			logger.debug(String.format("InformeDeshabilita: del esb httpStatus: %d", httpStatus));
		} catch (Exception e) {
			logger.error("InformeDeshabilita", e);
		}
	}

	private Long getCantidadSolicitada(Long sku, Map<String, Object> map) {
		ConsultaStockRequest requestStock = (ConsultaStockRequest) map.get(ServiciosdeBus.REQUEST_STOCK_KEY);
		for (Stock stock : requestStock.local[0].stock) {
			if (sku.equals(stock.codigoProducto))
				return stock.cantidad;
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
