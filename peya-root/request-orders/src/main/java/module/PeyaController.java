package module;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;

//import module.integracion.MantenedorStock;

/**
 *
 * @author mariano.rocha
 */
public class PeyaController {

    private static final String version = "1.2.19-sdk8 (07-12-2022)";
    protected static String log4jConfigFile = "pedidosYa_log4j.properties";
    private static Logger logger = Logger.getLogger(PeyaController.class);
    private static Properties integracionProps;
    
    public static void main(String[] args) throws ApiException, IOException {
    	Properties properties = new Properties();
    	properties.load(PeyaController.class.getClassLoader().getResourceAsStream("pedidosYa_log4j.properties"));
    	PropertyConfigurator.configure(properties);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy-HH:mm:ss.SSS");
        logger.info("[" + sdf.format(new Date()) + "] PeyaController - " +
                    version + " is running");

		integracionProps = new Properties();
		try (final java.io.InputStream stream =
				PeyaController.class.getClassLoader().getResourceAsStream("integracion.properties")){
					if (stream == null) throw new RuntimeException("stream es nulo");
					integracionProps.load(stream);
				}
    	String propAmbiente = System.getProperty("peya.requester.ambiente");
    	if (propAmbiente == null)
    		propAmbiente = integracionProps.getProperty("peya.requester.ambiente");
    	
    	String value = System.getProperty("peya.requester.usuario.api");
    	if (value != null)
    		integracionProps.setProperty("peya.requester.usuario.api", value);
    	
    	value = System.getProperty("peya.requester.secret.api");
    	if (value != null)
    		integracionProps.setProperty("peya.requester.secret.api", value);
    	
        logger.info(String.format("[ %s ] PeyaController - %s  is running a %s",
        		sdf.format(new Date()), version, propAmbiente));
        Token token = new Token();

        ApiClient apiClient = new ApiClient(token.getCentralizedConnection(propAmbiente));
        //////////////////////////////////////////////////////////////////////
        /*
        PartnerEvents partnerEvents = new PartnerEvents(apiClient);
        partnerEvents.getInitialization();
        partnerEvents.start();
        */
        //////////////////////////////////////////////////////////////////////

        //////////////////////////////////////////////////////////////////////

        Requester requester = new Requester (apiClient);
        logger.info("PeyaController: comienza proceso de ordenes");
        requester.getOrders();





    }

	public static Properties getIntegracionProps() {
		return integracionProps;
	}

}
