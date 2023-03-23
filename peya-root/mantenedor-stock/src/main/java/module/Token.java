package module;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.http.Credentials;
import com.pedidosya.reception.sdk.http.Environments;

/**
 *
 * @author mariano.rocha
 */
public class Token {

    private String client_id = "integration_ahumada_2"; //"integration_ahumada";
    private String client_secret ="1|%4c3Ybop"; // = "285owbaf!g";
    private Logger logger = Logger.getLogger(getClass());

    public Token(String client_id, String client_secret) {
        this.client_id = client_id;
        this.client_secret = client_secret;
        logger.info(String.format("Token_constructor: client_id: %s client_secret: %s", client_id, client_secret));
    }

    public Credentials getCentralizedConnection(String ambiente) {

        Credentials credentials = new Credentials();
        credentials.setClientId(client_id);
        credentials.setClientSecret(client_secret);

        if ("produccion".equalsIgnoreCase(ambiente))
        	credentials.setEnvironment(Environments.PRODUCTION);
        else
            credentials.setEnvironment(Environments.DEVELOPMENT);

        logger.info(String.format("Token: obtuvo credenciales para %s", client_id));
        return credentials;
    }

}
