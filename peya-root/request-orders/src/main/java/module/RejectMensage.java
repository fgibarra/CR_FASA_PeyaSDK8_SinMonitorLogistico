package module;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;
import com.pedidosya.reception.sdk.models.RejectMessage;
import java.util.List;

import org.apache.log4j.Logger;

public class RejectMensage {

    private ApiClient apiClient;
	private Logger logger = Logger.getLogger(getClass());

    public RejectMensage(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void getLogistic() throws ApiException {
        logger.info("Logistic");
        try {
            List<RejectMessage> rejectmensage = apiClient.getOrdersClient().getRejectMessagesClient().getAll();
            int j = 0;

            for (RejectMessage rm : rejectmensage) {

            }

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < rejectmensage.size(); i++) {

                if (rejectmensage.get(i).getForLogistics()) {
                    j++;
                    sb.append(j).append(") ");
                    sb.append(rejectmensage.get(i).getId()).append(":").append(rejectmensage.get(i).getDescriptionES());
                }
            }
            logger.info(sb.toString());
        } catch (Exception ex) {
            logger.error("getLogistic", ex);

        }
        //System.exit(0);
    }

    public void getMarketPlace() throws ApiException {

        try {

            logger.info("MarketPlace:");
            List<RejectMessage> rejectmensage = apiClient.getOrdersClient().getRejectMessagesClient().getAll();

            int j = 0;

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < rejectmensage.size(); i++) {
                if ((rejectmensage.get(i).getForPickup() == rejectmensage.get(i).getForLogistics()) || rejectmensage.get(i).getForPickup() == true) {
                    j++;
                    sb.append(j).append(") ");
                    sb.append(rejectmensage.get(i).getId()).append(":").append(rejectmensage.get(i).getDescriptionES());
                }
            }
            logger.info(sb.toString());
        } catch (Exception ex) {
            logger.error("getMarketPlace",ex);

        }
        //System.exit(0);

    }

    public void getPickup() throws ApiException {
        logger.info("Pickup");
        try {
            List<RejectMessage> rejectmensage = apiClient.getOrdersClient().getRejectMessagesClient().getAll();

            int j = 0;

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < rejectmensage.size(); i++) {
                if (rejectmensage.get(i).getForPickup() == true) {
                    j++;
                    sb.append(j).append(") ");
                    sb.append(rejectmensage.get(i).getId()).append(":").append(rejectmensage.get(i).getDescriptionES());
                }
            }
            logger.info(sb.toString());
        } catch (Exception ex) {
            logger.error("getPickup",ex);

        }

        //System.exit(0);
    }

}
