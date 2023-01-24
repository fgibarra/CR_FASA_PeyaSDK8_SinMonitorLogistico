package module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.clients.EventsClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;
import com.pedidosya.reception.sdk.models.Order;
import com.pedidosya.reception.sdk.queue.OnError;
import com.pedidosya.reception.sdk.queue.OnReceivedOrder;

import cl.ahumada.esb.dto.pharol.consultastock.ConsultaStockResponse;
import cl.ahumada.esb.dto.pharol.json.Stock;
import module.integracion.ServiciosdeBus;

public class Requester {

	private ApiClient apiClient;
	private Logger logger = Logger.getLogger(getClass());
	private ServiciosdeBus serviciosdeBus;

	public Requester(ApiClient apiClient) throws IOException {

		this.apiClient = apiClient;
		serviciosdeBus = new ServiciosdeBus(apiClient);
	}

	public void getOrders() throws ApiException {

		final EventsClient eventClient = apiClient.getEventClient();
		final Actions action = new Actions(apiClient);

		try {
			logger.info("waiting for new information ...");

			apiClient.getOrdersClient().getAll(new OnReceivedOrder() {

				@Override

				public boolean call(Order order) {

					try {

						logger.info("***********************************************************");
						logger.info("                    Order received");
						logger.info(String.format("ID: %d state: %s local: %s restaurantID=%d",
								order.getId(), order.getState(), order.getRestaurant().getIntegrationCode(),
								order.getRestaurant().getId()));
						logger.info("***********************************************************");

						if (("REJECTED".equals(order.getState()) || ("CONFIRMED".equals(order.getState())))) {
							logger.info("***********************************************************");
							logger.info("                    Order Update");
							logger.info(order.getId() + " - " + order.getState());
							logger.info("***********************************************************");
							logger.info("waiting for new information ...");
						}

						if ("PENDING".equals(order.getState())) {
							eventClient.reception(order);
							logger.info("***********************************************************");
							logger.info(order);
							logger.info("***********************************************************");

							eventClient.acknowledgement(order);
							// ir al ESB
							Map<String,Object> map = new HashMap<String,Object>();
							map.put("order", order);
							if (serviciosdeBus.confirma(map)) {
								logger.info(String.format("se CONFIRMA orden: %d", order.getId()));
								action.getConfirm(order);
								serviciosdeBus.actualizaHabilitacionProductos(map);
							} else {
								logger.info(String.format("se RECHAZA orden: %d", order.getId()));
								action.getReject(order);
								serviciosdeBus.informeRechazo(map);
								if (stockMenorCritico(map)) {
									serviciosdeBus.deshabilitaProductos(map);
								}
							}

						}
						// action.getReject(order);
					} catch (Exception ex) {
						logger.error("call", ex);
					}
					return true;
				}

			}, new OnError() {
				@Override
				public void call(ApiException ex) {
					logger.error("OnError", ex);
				};
			});
		} catch (Exception ex) {
			logger.error("Error en getOrders", ex);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean stockMenorCritico(Map<String, Object> map) {
		// si lo pedido menos el stock es mayor que el critico -> true
		List<Long> productosSinStock = (List<Long>) map.get(ServiciosdeBus.PRODUCTOS_SIN_STOCK_KEY);
		ConsultaStockResponse stockResponse = (ConsultaStockResponse)map.get("StockResponse");

		Integer stockCritico = serviciosdeBus.getStockCritico();
		List<Long> listaSacar = new ArrayList<Long>();

		for (Long sku : productosSinStock) {
			// busca lo que hay, si esto es mayor que el critico se saca de la lista de deshabilitar
			Long cantidadEnStock = buscaCantidadEnStock(sku, stockResponse);
			for (Stock pedido : stockResponse.local[0].stock) {
				long cpPedido = pedido.codigoProducto;
				if (sku == cpPedido) {
					//Long quedarian = pedido.cantidad - cantidadEnStock;
					if (/*cantidadEnStock == 0 ||*/ cantidadEnStock >= stockCritico && cantidadEnStock > 0) {
						// se deshabilita si lo que tengo es menor que el critico o no tengo nada
						logger.debug(String.format("stockMenorCritico: sku: %d cantidad pedida %d cantidad en stock %d stockCritico: %d NO se deshabilita",
								sku, pedido.cantidad, cantidadEnStock, stockCritico));
						listaSacar.add(sku);
					} else {
						logger.debug(String.format("stockMenorCritico: sku: %d cantidad pedida %d cantidad en stock %d stockCritico: %d se deshabilita",
								sku, pedido.cantidad, cantidadEnStock, stockCritico));
					}
					break;
				}
			}
		}
		for (Long skuSacar : listaSacar)
			productosSinStock.remove(skuSacar);

		if (productosSinStock.size() > 0) {
			StringBuffer sb = new StringBuffer();
			sb.append("VA a deshabilitar los siguientes sku:");
			for (Long sku : productosSinStock) {
				sb.append(String.format(" %d,", sku));
			}
			logger.debug(sb.toString());
		}
		return !productosSinStock.isEmpty();
	}

	public static Long buscaCantidadEnStock(Long sku, ConsultaStockResponse stockResponse) {
		for(Stock stock : stockResponse.local[0].stock) {

			if ( sku == stock.codigoProducto)
				return stock.cantidad;
		}
		return 0l;
	}
}
