
package module;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.clients.OrdersClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;
import com.pedidosya.reception.sdk.models.DeliveryTime;
import com.pedidosya.reception.sdk.models.Order;
import com.pedidosya.reception.sdk.models.RejectMessage;
import java.util.List;

import org.apache.log4j.Logger;

public class Actions {

    private ApiClient apiClient;
	private Logger logger = Logger.getLogger(getClass());

    public Actions(ApiClient apiClient) {
        this.apiClient = apiClient;

    }

    public void getConfirm(Order order) throws ApiException {
        OrdersClient ordersClient = apiClient.getOrdersClient();

        final List<DeliveryTime> deliveryTimes = apiClient.getOrdersClient().getDeliveryTimesClient().getAll();

        if (order.getLogistics() || order.getPickup()) {

            try {
                apiClient.getOrdersClient().confirm(order);
            } catch (ApiException ex) {
                logger.error("Error to cofirm", ex);
            }

        } else {//MKP MarketPlace.
            ordersClient.confirm(order, deliveryTimes.get(0));


        }
    }

    public void getReject(Order order) throws ApiException {
        try {
			OrdersClient ordersClient = apiClient.getOrdersClient();

			RejectMessage et = getRejectId(7l);
			if (et != null) {
				logger.debug(String.format("getReject: getDescriptionES():%s getDescriptionPT(): %s, getForLogistics(): %b, getForPickup(): %b, getId(): %d",
						et.getDescriptionES(), et.getDescriptionPT(), et.getForLogistics(), et.getForPickup(), et.getId()));
				ordersClient.reject(order, et);
			} else {
				throw new RuntimeException("No se encontro id 7 entre los posible RejectMessage");
			}
		} catch (Exception e) {
			logger.error(String.format("getReject: getId(): %d", order.getId()), e);
		}

    }

	private RejectMessage getRejectId(long i) {
    	List<RejectMessage> rejectMessages = null;
		try {
			rejectMessages = apiClient.getOrdersClient().getRejectMessagesClient().getAll();
			for (RejectMessage et : rejectMessages)
				if (et.getId() == i)
					return et;
		} catch (ApiException e) {
			logger.error(String.format("getRejectId: buscando id %d",i), e);
		}
		return null;
	}

}
