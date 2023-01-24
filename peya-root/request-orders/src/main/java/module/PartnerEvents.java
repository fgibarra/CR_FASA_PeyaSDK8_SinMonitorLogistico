package module;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;
import com.pedidosya.reception.sdk.models.Restaurant;
import com.pedidosya.reception.sdk.utils.PaginationOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class PartnerEvents extends Thread {

    private final ApiClient apiClient;
	private Logger logger = Logger.getLogger(getClass());
	private Long periodo;

    public PartnerEvents(ApiClient apiClient) {
        this.apiClient = apiClient;
        String strPeriodo = System.getProperty("peya.periodo");
        this.periodo = strPeriodo != null ? Long.valueOf(strPeriodo):30000l;
    }

    @Override
    public void run() {

            try {

                PaginationOptions options = PaginationOptions.create();
                List<Restaurant> partners = new ArrayList<>();
                List<Restaurant> newPartners = apiClient.getRestaurantsClient().getAll(options);
                partners.addAll(newPartners);

                while (newPartners.size() != 0) {
                    newPartners = apiClient.getRestaurantsClient().getAll(options.next());
                    partners.addAll(newPartners);
                }

                for (int i = 0; i < partners.size(); i++) {
                    final Restaurant partner = partners.get(i);

                    TimerTask task;
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                long restaurantId = partner.getId();
                                apiClient.getEventClient().heartBeat(restaurantId);
                            } catch (ApiException ex) {
                                logger.error("Error in hearbeat", ex);
                            } catch (Exception ex) {
                            	logger.error("Error in hearbeatt", ex);
                            }
                        }
                    };

                    new Timer().schedule(task, 0, periodo);

                }
            } catch (ApiException ex) {
                System.out.println(ex);
            }
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
