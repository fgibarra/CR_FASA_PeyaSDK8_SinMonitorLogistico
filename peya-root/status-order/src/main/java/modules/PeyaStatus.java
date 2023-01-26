package modules;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.http.Environments;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
/**
 * Solicita status de una orden que ha sido confirmada y esta en etapa de despacho
 * 
 * Para habilitar ambiente donde apuntar hay que definir propiedad ambiente = desarrollo | produccion
 *
 */
@SuppressWarnings("unused")
public class PeyaStatus 
{
    private static final String version = "1.0.0 (19-10-2021)";
    protected static String log4jConfigFile = "pedidosYa_log4j.properties";
    private static Logger logger = Logger.getLogger(PeyaStatus.class);
    private static Properties integracionProps;
    private static ApiClient apiClient;
    private static ReactiveFullDuplexServer server;

    public static void main( String[] args ) throws IOException
    {
    	Properties properties = new Properties();
    	properties.load(PeyaStatus.class.getClassLoader().getResourceAsStream("pedidosYa_log4j.properties"));
    	PropertyConfigurator.configure(properties);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy-HH:mm:ss.SSS");

        integracionProps = new Properties();
		try (final java.io.InputStream stream =
		           PeyaStatus.class.getClassLoader().getResourceAsStream("integracion.properties")){
					if (stream == null) throw new RuntimeException("stream es nulo");
					integracionProps.load(stream);
				}
        
        String propAmbiente = System.getProperty("peya.status.ambiente");
        if (propAmbiente == null)
        	propAmbiente = integracionProps.getProperty("peya.status.ambiente");

    	String value = System.getProperty("peya.status.usuario.api");
    	if (value != null)
    		integracionProps.setProperty("peya.status.usuario.api", value);
    	
    	value = System.getProperty("peya.status.secret.api");
    	if (value != null)
    		integracionProps.setProperty("peya.status.secret.api", value);
    	
        logger.info(String.format("[ %s ] Peya Status - %s  is running a %s",
        		sdf.format(new Date()), version, propAmbiente));
        
        Token token = new Token();
        apiClient = new ApiClient(token.getCentralizedConnection(propAmbiente));

        try {
            server = new ReactiveFullDuplexServer(apiClient, integracionProps);
        } catch (Exception exception) {
            logger.error("", exception);
        }


    }

	public static Properties getIntegracionProps() {
		return integracionProps;
	}
}
