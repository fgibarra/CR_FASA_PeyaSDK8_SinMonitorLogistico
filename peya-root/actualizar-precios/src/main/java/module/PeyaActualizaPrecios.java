package module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.pedidosya.reception.sdk.ApiClient;

import module.integracion.MantenedorPrecios;
import module.integracion.dto.ProductosFasa;
import module.integracion.utils.JsonUtilities;

public class PeyaActualizaPrecios {

	private static final String version = "1.0.1 (08-03-2023)";
    private static String properyFileName = "integracion.properties";
	private Properties integracionProps = null;
	private ApiClient apiClient = null;
	private String idServidor = null;
	private String numThreads = null;
	
	protected static String log4jConfigFile = "actualizaPrecios_log4j.properties";
	private static Logger logger = Logger.getLogger(PeyaActualizaPrecios.class);

	public static PeyaActualizaPrecios instance = null;
	
	public static PeyaActualizaPrecios getInstance() {
		if (instance == null)
			instance = new PeyaActualizaPrecios();
		return instance;
	}
	
	public PeyaActualizaPrecios() {
		super();
		try {
			Properties properties = new Properties();
			properties.load(PeyaActualizaPrecios.class.getClassLoader()
					.getResourceAsStream("actualizaPrecios_log4j.properties"));
			PropertyConfigurator.configure(properties);

	        integracionProps = initFromProperties(properyFileName);
	        if (integracionProps == null) {
				this.integracionProps = new Properties();
				try (final java.io.InputStream stream = PeyaActualizaPrecios.class.getClassLoader()
						.getResourceAsStream("integracion.properties")) {
					if (stream == null)
						throw new RuntimeException("stream es nulo");
					integracionProps.load(stream);
				}
	        }
			String propAmbiente = System.getProperty("peya.actualizaPrecios.ambiente");
			if (propAmbiente == null)
				propAmbiente = integracionProps.getProperty("peya.actualizaPrecios.ambiente");
			this.idServidor = System.getProperty("peya.actualizaPrecios.idServidor");
			if (idServidor == null)
				idServidor = integracionProps.getProperty("peya.actualizaPrecios.idServidor");
			this.numThreads = integracionProps.getProperty("peya.actualizaPrecios.numThreads");
			if (numThreads == null)
				numThreads = integracionProps.getProperty("peya.actualizaPrecios.numThreads");

			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy-HH:mm:ss.SSS");
	        logger.info(String.format("[ %s ] PeyaActualizaPrecios - %s  is running a %s - Propiedades definidas en: %s\n%s\n",
	        		sdf.format(new Date()), version, propAmbiente, properyFileName, integracionProps));

	        Token token = new Token(integracionProps);
//			this.apiClient = new ApiClient(token.getCentralizedConnection(propAmbiente));

		} catch (IOException e) {
			System.out.println("No pudo leer archivos de propiedades");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		PeyaActualizaPrecios obj = getInstance();
		MantenedorPrecios mantenedor = null;
		
		try {
			/*
			Object valores[] = {"gtin","INTEGRATION_CODE" ,"INTEGRATION_NAME",
					Double.valueOf(1500), "DESCRIPION", "IMAGE", "NOMBRE", Boolean.TRUE, Boolean.FALSE,
					"MEASUREMENT_UNIT", Double.valueOf(3), "PRESCRIPTION_BEHAVIOUR", 
					"SECTION_INTEGRATION_CODE", "SECTION_NAME", "VENDOR_ID",
					Integer.valueOf(1)
			};
			ProductosFasa pf = new ProductosFasa(Arrays.asList(valores));
			JsonUtilities util = JsonUtilities.getInstance();
			String json = util.toJsonString(ProductosFasa.class, pf);
			logger.info(json);
			*/
			mantenedor = new MantenedorPrecios(obj.getApiClient(), obj.getIdServidor(), obj.getNumThreads(), getIntegracionProps());
		} catch (Exception e) {
			logger.error("Termino con error de inicializacion", e);
			System.exit(1);
		}

		mantenedor.actualizaPrecios();
		
		System.exit(0);
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
		return getInstance().integracionProps;
	}

	public ApiClient getApiClient() {
		return apiClient;
	}

	public String getIdServidor() {
		return idServidor;
	}

	public String getNumThreads() {
		return numThreads;
	}

}
