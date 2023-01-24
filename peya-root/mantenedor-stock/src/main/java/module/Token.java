package module;

import com.pedidosya.reception.sdk.http.Credentials;
import com.pedidosya.reception.sdk.http.Environments;

/**
 *
 * @author mariano.rocha
 */
public class Token {

    private Credentials CREDENTIALS;

    private static final String client_id = "integration_ahumada";
    private static final String client_secret = "285owbaf!g";

    public Token() {
        this.CREDENTIALS = CREDENTIALS;

    }

    public Credentials getCentralizedConnection(String ambiente) {

        Credentials credentials = new Credentials();
        credentials.setClientId(client_id);
        credentials.setClientSecret(client_secret);

        if ("produccion".equalsIgnoreCase(ambiente))
        	credentials.setEnvironment(Environments.PRODUCTION);
        else
            credentials.setEnvironment(Environments.DEVELOPMENT);


        return credentials;
    }

}
