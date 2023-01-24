package module.integracion;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.models.Address;
import com.pedidosya.reception.sdk.models.Detail;
import com.pedidosya.reception.sdk.models.Discount;
import com.pedidosya.reception.sdk.models.Order;
import com.pedidosya.reception.sdk.models.Payment;
import com.pedidosya.reception.sdk.models.User;

import cl.ahumada.esb.dto.pharol.json.Local;
import cl.ahumada.esb.dto.pharol.json.Stock;
import cl.ahumada.esb.dto.pharolV2.json.CarroCompras;
import cl.ahumada.esb.dto.pharolV2.json.Cliente;
import cl.ahumada.esb.dto.pharolV2.json.DatosEntrega;
import cl.ahumada.esb.dto.pharolV2.json.Descuento;
import cl.ahumada.esb.dto.pharolV2.json.MedioPago;
import cl.ahumada.esb.dto.pharolV2.json.Producto;
import cl.ahumada.esb.dto.pharolV2.orquestadorDescuentos.comun.Message;
import cl.ahumada.esb.dto.pharolV4.pedidos.PedidosRequest;

public class ObjectFactory {

	private Logger logger = Logger.getLogger(getClass());
	Long totalBoleta;
	private Properties integracionProps;

	public Local factoryLocal(Order order) {
		// generar el request para consultar stock

		String valor = order.getRestaurant().getIntegrationCode();
		if (valor == null)
			throw new RuntimeException("Aun no estan asignados los numero de local");
		Long numeroLocal = Long.valueOf(valor);
		List<Stock> stock = new ArrayList<Stock>();
		for (Detail detail : order.getDetails()) {
			Long codigoProducto = Long.valueOf(detail.getProduct().getIntegrationCode());
			Long cantidad = (long)detail.getQuantity();
			stock.add(new Stock(codigoProducto, cantidad));
		}
		return new Local(numeroLocal, stock.toArray(new Stock[0]));
	}

	public PedidosRequest factoryPedidosRequest(Map<String,Object> map) {
		Order order = (Order) map.get("order");
		integracionProps = (Properties) map.get("integracionProps");
		PedidosRequest request = null;
		long idTransaccion = order.getId();
//		long idTransaccion = new java.util.Date().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		String fecha = sdf.format(new java.util.Date());
		String valor = order.getRestaurant().getIntegrationCode();
		if (valor == null)
			throw new RuntimeException("Aun no estan asignados los numero de local");
		Long numeroLocal = Long.valueOf(valor);
		Long costoDespacho = order.getPayment().getShippingNoDiscount().longValue();
		CarroCompras carroCompras = factoryCarroCompras(order);

		MedioPago[] medioPago = factoryMedioPago(order.getPayment());
		Cliente cliente = factoryCliente(order.getUser());
		DatosEntrega datosEntrega = factoryDatosEntrega(order);
		Message[] posMessages = null;

		logger.debug(String.format("factoryPedidosRequest: idTransaccion:%s fecha:%s numeroLocal=%s costoDespacho:%s carroCompras:%s, medioPago=%s cliente=%s datosEntrega=%s",
				idTransaccion, fecha, numeroLocal,
				costoDespacho, carroCompras, medioPago[0],cliente, datosEntrega));

		try {
			request = new PedidosRequest("PEDIDOSYA", idTransaccion, 0l, fecha, numeroLocal,
					costoDespacho,carroCompras, medioPago, cliente, datosEntrega, posMessages);
		} catch (Exception e) {
			logger.error(String.format("factoryPedidosRequest: idTransaccion:%s fecha:%s numeroLocal=%s costoDespacho:%s carroCompras:%s, medioPago=%s cliente=%s datosEntrega=%s clase=%s",
				idTransaccion, fecha, numeroLocal,
				costoDespacho, carroCompras, medioPago[0],cliente, datosEntrega, request.getClass().getSimpleName()), e);
		}

		return request;
	}

	public CarroCompras factoryCarroCompras(Order order) {
		Producto producto[] = factoryProducto(order.getDetails());
		//Payment payment = order.getPayment();

		Long descuentoTotal = getDescuento(order.getDiscounts()); // ??????????????????

		Long neto = 0l; //payment.getTotal() != null ? payment.getTotal().longValue() : 0;
		Long iva = 0l; //payment.getTax() != null ? payment.getTax().longValue() : 0;
		totalBoleta = sumatoria(order.getDetails(), order.getDiscounts());
		CarroCompras carroCompra = new CarroCompras(producto, descuentoTotal, neto, iva, totalBoleta);
		return carroCompra;
	}

	private Long sumatoria(List<Detail> details, List<Discount> discounts) {
		Long valor = 0l;
		String descuentosAplicables = 	integracionProps.getProperty("descuentosAplicables");
		for (Detail detail : details) {
			valor += detail.getSubtotal() != null ? detail.getSubtotal().longValue() : 0l;
		}
		for (Discount discount : discounts) {
			String codigoDescuento = discount.getCode();
			if (descuentosAplicables.indexOf(codigoDescuento) >= 0) {
				valor -= discount.getAmount() != null ? discount.getAmount().longValue() : 0l;
			}
		}
		return valor;
	}

	public Long getDescuento(List<Discount> discounts) {
		long totalDescuentos = 0l;
		if (discounts != null) {
			for (Discount discount : discounts) {
				String codigoDescuento = discount.getCode();
				String descuentosAplicables = 	integracionProps.getProperty("descuentosAplicables");

				if (descuentosAplicables.indexOf(codigoDescuento) >= 0)
					totalDescuentos += discount.getAmount().longValue();
			}
		}
		return totalDescuentos;
	}

	public Producto[] factoryProducto(List<Detail> details) {
		Map<Object, Producto> mapSkus = new HashMap<Object,Producto>();

		List<Producto> productos = new ArrayList<Producto>();
		for (Detail detail : details) {
			Object codigoProducto = detail.getProduct().getIntegrationCode().toString().trim();
			Long cantidad = detail.getQuantity().longValue();
			Long precioUnitario = detail.getUnitPrice().longValue();
			Long total = detail.getSubtotal().longValue();
			Descuento[] descuentos = factoryDescuentos(detail);

			Producto pAnterior = mapSkus.get(codigoProducto);
			if (pAnterior != null) {
				cantidad += pAnterior.cantidad;
				total += pAnterior.total;
			} else {
				Producto nProducto = new Producto(codigoProducto, cantidad, precioUnitario, total, descuentos);
				productos.add(nProducto);
				mapSkus.put(codigoProducto, nProducto);
			}
		}
		return productos.toArray(new Producto[0]);
	}

	public Descuento[] factoryDescuentos(Detail detail) {
		String type = "DP";
		long valorDescuento = detail.getDiscount() != null ? detail.getDiscount().longValue() : 0;
		if (valorDescuento == 0)
			return null;
		Object codigoDescuento = "300000";
		String descripcionDescuento = "PEDIDOSYA";
		boolean aplicar = true;
		Descuento descuento = new Descuento(type, valorDescuento, codigoDescuento, descripcionDescuento, aplicar);
		return new Descuento[] { descuento };
	}

	public MedioPago[] factoryMedioPago(Payment payment) {
		List<MedioPago> medio = new ArrayList<MedioPago>();
		int formaPago = 3;
//		int monto = payment.getAmountNoDiscount().intValue();
		int monto = totalBoleta.intValue();
		medio.add(new MedioPago(formaPago, monto, 0, "0", "0", 0));
		return medio.toArray(new MedioPago[0]);
	}

	public Cliente factoryCliente(User user) {
		String nombres = user.getName();
		String apellidos = user.getLastName();
		String rut = user.getIdentityCard() != null && user.getIdentityCard().length() > 0 ? user.getIdentityCard() : "762114259";
		String mail = user.getEmail();
		return new Cliente (nombres, apellidos, rut, mail);
	}

	public DatosEntrega factoryDatosEntrega(Order order) {
		Address address = order.getAddress();
		String telefono = address.getPhone();
		String calle = address.getStreet();
		String numero = address.getDoorNumber();
		String dpto = address.getComplement();
		if (dpto != null)
			dpto = dpto.replace('|', ' ');
		String comuna = address.getArea();
		String region = address.getCity();
		String tipoEntrega = "1";
		java.util.Date registeredDate = order.getRegisteredDate();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		String ruta = sdf.format(registeredDate);
		java.util.Date deliveryDate = order.getDeliveryDate();
		String fechaEntregaDesde = sdf.format(deliveryDate);
		java.util.Date entregaDate = new java.util.Date((deliveryDate.getTime()+10*60*1000l));
		String fechaEntregaHasta = sdf.format(entregaDate);
		return new DatosEntrega(telefono,calle, numero, dpto, comuna, region, tipoEntrega, ruta, fechaEntregaDesde, fechaEntregaHasta);
	}

}
