package heartbeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;
import com.pedidosya.reception.sdk.models.Restaurant;
import com.pedidosya.reception.sdk.utils.PaginationOptions;

public class PartnerEvents extends Thread {

    private final ApiClient apiClient;
	private Logger logger = Logger.getLogger(getClass());

    public PartnerEvents(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public void run() {

    	List<Restaurant> partners = getRestaurantAbiertos();
    	logger.debug("----------------------------------------------------------------------------------------------");
		logger.debug(String.format("Recupero %d locales para procesar heartbeat", partners.size()));
		for (int i = 0; i < partners.size(); i++) {
		    final Restaurant partner = partners.get(i);
		    logger.debug(String.format("heartbeat para i=%d local %d", i, partner.getId()));
		    TimerTask task;
		    task = new TimerTask() {
		        @Override
		        public void run() {
		            try {
		                long restaurantId = partner.getId();
		                logger.info(String.format("Heartbeat antes de invocar heartbeat para %d", restaurantId));
		                apiClient.getEventClient().heartBeat(restaurantId);
		                logger.info(String.format("Heartbeat despues de invocar heartbeat para %d", restaurantId));
		            } catch (ApiException ex) {
		                logger.error("Error in hearbeat", ex);
		            } catch (Exception ex) {
		            	logger.error("Error in hearbeat", ex);
		            }
		        }
		    };

		    new Timer().schedule(task, 0, 180000);

		}
    }

	private List<Restaurant> getRestaurantAbiertos() {
		List<Restaurant> partners = new ArrayList<Restaurant>();
		
		// TODO invocar a la base para recuperar los locales abiertos
		
		
		return partners;
	}

    public void getInitialization() throws ApiException {

        PaginationOptions options = PaginationOptions.create();
        List<Restaurant> partners = new ArrayList<>();
        List<Restaurant> newPartners = apiClient.getRestaurantsClient().getAll(options);
        partners.addAll(newPartners);

        while (newPartners.size() != 0) {
            newPartners = apiClient.getRestaurantsClient().getAll(options.next());

        }
        partners.addAll(newPartners);
        Map<String, Object> version = new HashMap<>();
        version.put("os", "Linux");
        version.put("app", "Integracion Ahumada 1.0");

        for (Restaurant partner : partners) {
            long restaurantId = partner.getId();
            apiClient.getEventClient().initialization(version, restaurantId);
            logger.info("Partners with Integration : " + partner.getName());
        }

    }
}
