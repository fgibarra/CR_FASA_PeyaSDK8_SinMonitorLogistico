package module.integracion;

import java.util.Properties;

import com.pedidosya.reception.sdk.ApiClient;

import module.integracion.bd.ErrorBaseDatos;
import module.integracion.bd.PoolManager;
import module.integracion.threads.ProcesaMantencion;
import module.integracion.threads.Semaforo;

/**
 * @author fernando
 *
 * Wrapper para la funcion de Mantencion de precios
 * - Crea el pool de conexiones a la BD para que sean usadas por los distintos threads que hacen el proceso
 * - Instancia tantos threads como los indicados en los argumentos de inicio o en el archivo de propiedades y 
 *   espera a que termine su ejecucion.
 */
public class MantenedorPrecios {

	private ApiClient apiClient;
	private String idServidor;
	private Integer numThreads = 1;
	private Properties integracionProps;
	private final PoolManager poolManager;
    public static Semaforo waitSem;
	private static int threadsRunning;
	
	public MantenedorPrecios(ApiClient apiClient, String idServidor, String numThreads, Properties integracionProps) throws ErrorBaseDatos {
		this.apiClient = apiClient;
		this.idServidor = idServidor;
		this.integracionProps = integracionProps;
		try {
			this.numThreads = Integer.valueOf(numThreads);
		} catch (Exception e) {
			;
		}
		this.poolManager = PoolManager.getInstance(integracionProps);
		waitSem = new Semaforo(0);
	}

	/**
	 * Instancia Threads
	 */
	public void actualizaPrecios() {
		// activa los thread
		setThreadsRunning(getNumThreads());
		for (int i=0; i < getNumThreads(); i++)
			(new ProcesaMantencion(apiClient, idServidor, numThreads, integracionProps, poolManager, Integer.valueOf(i))).start();
		
		// espera que terminen todos antes de volver
		waitSem.espere();
	}

	public static synchronized void decrementThreadsRunning() {
		threadsRunning--;
		if (threadsRunning == 0)
			waitSem.continuar();
	}
	//===============================================================================================
	// Getters y Setters
	//===============================================================================================
	
	public ApiClient getApiClient() {
		return apiClient;
	}

	public String getIdServidor() {
		return idServidor;
	}

	public Integer getNumThreads() {
		return numThreads;
	}

	public static void setThreadsRunning(int threadsRunning) {
		MantenedorPrecios.threadsRunning = threadsRunning;
	}

}
