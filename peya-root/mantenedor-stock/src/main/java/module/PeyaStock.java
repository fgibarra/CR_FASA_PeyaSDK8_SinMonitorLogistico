package module;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;

import module.integracion.MantenedorStock;

/**
 *
 * @author mariano.rocha
 */
public class PeyaStock {

    private static final String version = "1.0.8 (07-12-2022)";
    protected static String log4jConfigFile = "pedidosYa_log4j.properties";
    private static Logger logger = Logger.getLogger(PeyaStock.class);

    public static void main(String[] args) throws ApiException, IOException {
		// crear archivo para datos
    	Properties properties = new Properties();
    	properties.load(PeyaStock.class.getClassLoader().getResourceAsStream("pedidosYa_log4j.properties"));
    	PropertyConfigurator.configure(properties);

		Properties integracionProps = new Properties();
		try (final java.io.InputStream stream =
				PeyaStock.class.getClassLoader().getResourceAsStream("integracion.properties")){
					if (stream == null) throw new RuntimeException("stream es nulo");
					integracionProps.load(stream);
				}
    	String propAmbiente = System.getProperty("peya.stock.ambiente");
    	if (propAmbiente == null)
    		propAmbiente = integracionProps.getProperty("peya.stock.ambiente");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy-HH:mm:ss.SSS");
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

        MantenedorStock mantenedorStock = new MantenedorStock(apiClient);
        mantenedorStock.actualizaStock();

    }

}
