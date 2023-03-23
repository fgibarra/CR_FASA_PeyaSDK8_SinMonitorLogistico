package module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    private static final String version = "1.0.11 (20-03-2023)";
    protected static String log4jConfigFile = "pedidosYa_log4j.properties";
    private static String properyFileName = "integracion.properties";
	private static Properties integracionProps = null;
    private static Logger logger = Logger.getLogger(PeyaStock.class);

    public static void main(String[] args) throws ApiException, IOException {
		// crear archivo para datos
    	Properties properties = new Properties();
    	properties.load(PeyaStock.class.getClassLoader().getResourceAsStream("pedidosYa_log4j.properties"));
    	PropertyConfigurator.configure(properties);

    	integracionProps = initFromProperties(properyFileName);
        if (integracionProps == null) {
			integracionProps = new Properties();
			try (final java.io.InputStream stream =
					PeyaStock.class.getClassLoader().getResourceAsStream("integracion.properties")){
						if (stream == null) throw new RuntimeException("stream es nulo");
						integracionProps.load(stream);
					}
        }
    	String propAmbiente = System.getProperty("peya.stock.ambiente");
    	if (propAmbiente == null)
    		propAmbiente = integracionProps.getProperty("peya.stock.ambiente");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy-HH:mm:ss.SSS");
        logger.info(String.format("[ %s ] PeyaStock - %s  is running a %s - Propiedades definidas en: %s\n%s\n",
        		sdf.format(new Date()), version, propAmbiente, properyFileName, integracionProps));

        Token token = new Token(integracionProps.getProperty("peya.mantenedorStock.usuario.api"), 
        		integracionProps.getProperty("peya.mantenedorStock.secret.api"));
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

    protected static Properties initFromProperties(String propertiesFileName) {
        Properties idProps = new Properties();

        logger.debug(String.format("initFromProperties: busca properties en %s/%s", System.getProperty("user.dir"),propertiesFileName));
        File fileProperties = new File(String.format("%s/%s", System.getProperty("user.dir"),propertiesFileName));
        if (!fileProperties.exists()) {
            fileProperties =  new File(String.format("%s/src/main/%s", System.getProperty("user.dir"),propertiesFileName));
        }
        logger.debug(String.format("fileProperties= %s existe %b", fileProperties.getAbsolutePath(), fileProperties.exists()));

        if (fileProperties.exists()) {
            try {
                InputStream is = null;
                try {
                    is = new FileInputStream(fileProperties);
                } catch (FileNotFoundException e) {
                    is = null;
                }
                if (is != null) {

                    try {
                        idProps.load(is);
                    } catch (Exception e) {
                        logger.error("cargando properties", e);
                    }
                }
            } catch (Exception e) {
                logger.error(String.format("initFromProperties: %s", e.getMessage()), e);
            }
        }


        return idProps;
    }

	public static Properties getIntegracionProps() {
		return integracionProps;
	}


}
