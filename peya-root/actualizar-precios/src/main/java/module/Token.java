package module;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.http.Credentials;
import com.pedidosya.reception.sdk.http.Environments;

public class Token {

    private String client_id = "integration_ahumada_2"; //"integration_ahumada";
    private String client_secret ="1|%4c3Ybop"; // = "285owbaf!g";
    private Logger logger = Logger.getLogger(getClass());
    
    public Token(Properties integracionProps) {
        this.client_id = integracionProps.getProperty("peya.requester.usuario.api");
        this.client_secret = integracionProps.getProperty("peya.requester.secret.api");
    }

    public Credentials getCentralizedConnection(String ambiente) {

        Credentials credentials = new Credentials();
        credentials.setClientId(client_id);
        credentials.setClientSecret(client_secret);

        logger.debug(String.format("getCentralizedConnection: client_id: %s client_secret: %s" , client_id, client_secret));
        
        if ("produccion".equalsIgnoreCase(ambiente))
        	credentials.setEnvironment(Environments.PRODUCTION);
        else
            credentials.setEnvironment(Environments.DEVELOPMENT);


        return credentials;
    }
}
