package module.integracion;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.clients.ProductsClient;
import com.pedidosya.reception.sdk.models.Order;
import com.pedidosya.reception.sdk.models.Product;

import cl.ahumada.esb.dto.pedidosYa.consultaStock.json.Items;
import cl.ahumada.esb.dto.pharol.consultastock.ConsultaStockRequest;
import cl.ahumada.esb.dto.pharol.json.Local;
import cl.ahumada.esb.dto.pharolV4.pedidos.PedidosRequest;
import cl.ahumada.esb.utils.json.JSonUtilities;

public class ServiciosdeBus {

	private ApiClient apiClient;
	private InvocaESB invocaESB;
	private Properties integracionProps;

	public static String ORDER_KEY = "order";
	public static String PRODUCTOS_SIN_STOCK_KEY = "productosSinStock";
	public static String REQUEST_STOCK_KEY = "requestStock";
	public static String REQUEST_PEDIDOS_KEY = "requestPedidos";
	public static String RESULTADO_OPERACION_KEY = "resultadoOperacion";
	public static String STOCK_CRITICO_KEY = "stock_critico";
	public static String ENDPOINT_MANTIENE_STOCK_KEY = "endpoint_ws_mantiene_stock";
	public static String ENDPOINT_INFORMA_DESHABILITA_KEY = "endpoint_ws_informeDeshabilita";

	public ServiciosdeBus(ApiClient apiClient) throws IOException {
		super();
		this.apiClient = apiClient;
		integracionProps = new Properties();
		try (final java.io.InputStream stream =
		           this.getClass().getClassLoader().getResourceAsStream("integracion.properties")){
					if (stream == null) throw new RuntimeException("stream es nulo");
					integracionProps.load(stream);
				}
		invocaESB = new InvocaESB(integracionProps);
	}

	private ObjectFactory factory = new ObjectFactory();
	private Logger logger = Logger.getLogger(getClass());

	/**
	 * 1.- arma request a restapi ConsultaStock
	 * 2.- invoca al api de stock
	 * 3.- si no hay stock, retorna false
	 * 4.- arma request a restapi de pedidosV4
	 * 5.- invoca api de pedidos
	 * 6.- retorna true si pudo realizar el pedido
	 * @param map
	 * key:
	 * 		order, com.pedidosya.reception.sdk.models.Order - in
	 * 		requestStock, ConsultaStockRequest - out (usado para actualizaHabilitacionProductos)
	 * 		productosSinStock, List<Long>      - out (usado para deshabilitaProductos)
	 * @return
	 */
	public boolean confirma(Map<String,Object> map) {
		Order order = (Order) map.get(ORDER_KEY);
		Long restaurantID = order.getRestaurant().getId();
		map.put("restaurantID", restaurantID);
		map.put("integracionProps", integracionProps);

		boolean confirma = false;
		try {
			Local local = factory.factoryLocal(order);

			ConsultaStockRequest requestStock = new ConsultaStockRequest(new Local[] {local});
			String json = "no pudo";
			try {
				json = JSonUtilities.getInstance().java2json(requestStock);
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
			logger.info(String.format("request Stock: %s", json));
			map.put(REQUEST_STOCK_KEY, requestStock);

			// invocar servicio consulta stock
			Boolean hayStock = invocaESB.hayStock(map);
			if (!hayStock) {
				return confirma;
			}

			// si hay stock
			confirma = true;
			// invocar servicio pedidos
			PedidosRequest requestPedidos = factory.factoryPedidosRequest(map);
			if (requestPedidos.carroCompras != null) {
				map.put(REQUEST_PEDIDOS_KEY, requestPedidos);
				json = "no pudo";
				try {
					json = JSonUtilities.getInstance().java2json(requestPedidos);
				} catch (JsonProcessingException e) {
					logger.error("", e);
				}
				logger.info(String.format("request Pedido: %s", json));

				confirma = invocaESB.realizarPedido(map);
			}
		} catch (Exception e) {
			logger.error("confirma", e);
		}
		return confirma;
	}

	/**
	 * @param map
	 */
	public void actualizaHabilitacionProductos(Map<String,Object> map) {
		// reprocesar la consulta de stock para reinicializar los productos sin stock
		map.remove(PRODUCTOS_SIN_STOCK_KEY);
		// consultar stock que quedo despues del pedido
		// verificar el stock contra stock critico
		Boolean hay = invocaESB.hayStockConReserva(map);

		// si hay producto bajo stock critico deshabilitarlo
		if (!hay)
			deshabilitaProductos(map);
	}

	public void deshabilitaProductos(Map<String, Object> map) {
		@SuppressWarnings("unchecked")
		List<Long> productosSinStock = (List<Long>) map.get(PRODUCTOS_SIN_STOCK_KEY);
		Long restaurantID = (Long) map.get("restaurantID");

		if (productosSinStock != null && productosSinStock.size() > 0) {
			for (Long codigoProducto : productosSinStock) {
				String integrationCode = String.format("%d", codigoProducto);
				try {
					logger.debug(String.format("deshabilitaProductos: invoca apiClient.getProductsClient().disable(%s)", integrationCode));
					ProductsClient productsClient = apiClient.getProductsClient();
					Product producto = (Product) productsClient.getByIntegrationCode(integrationCode, restaurantID);
					producto.setEnabled(false);
					apiClient.getProductsClient().update(producto, restaurantID);
				} catch (Exception e) {
					logger.error(String.format("deshabilitaProductos: integrationCode: %s",integrationCode), e);
				}
			}
		}
	}

	public Items[] getItemsDeshabilitar() {
		return invocaESB.invocaWsMantieneStock(0);
	}

	public Items[] getItemsHabilitar() {
		return invocaESB.invocaWsMantieneStock(1);
	}

	public Integer getStockCritico() {
		return invocaESB.getStockCritico();
	}

	public Properties getIntegracionProps() {
		return integracionProps;
	}

}
