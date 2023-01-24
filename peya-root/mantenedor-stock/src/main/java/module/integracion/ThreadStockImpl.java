package module.integracion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.clients.ProductsClient;
import com.pedidosya.reception.sdk.models.Product;

import cl.ahumada.esb.dto.pedidosYa.consultaStock.json.Items;

public class ThreadStockImpl extends Thread implements ThreadStock {

	private ApiClient apiClient;
	private String funcion;
	private List<Items> datos;
	private Logger logger = Logger.getLogger(getClass());
	private boolean working=false, mustStop=false;
	private List<ThreadStock> threads;
	private Semaforo waitSem;
	private int threadNum;
	public int getThreadNum() {
		return threadNum;
	}

	private File flogfile;
	private BufferedWriter fd = null;

	public ThreadStockImpl(ApiClient apiClient,
			Semaforo waitSem,
			List<ThreadStock> threads,
			String funcion,
			List<Items> datos,
			int threadNum) {
		super();
		this.apiClient = apiClient;
		this.waitSem = waitSem;
		this.threads = threads;
		this.funcion = funcion;
		this.datos = datos;
		this.threadNum = threadNum;

		// crear archivo para datos
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

		String flogname = String.format("transacciones_%s_%s_%d", funcion,sdf.format(new Date()),threadNum);
		this.flogfile = new File(String.format("%s/log", System.getProperty("user.dir")), flogname);
		logger.info(String.format("archivo contenido trx: %s", flogfile.getAbsolutePath()));
	}

	public void run() {
		working = true;
        do {
            doJob();
            if (mustStop)
                setWorking(false);
        } while (isWorking());
        // determinar si es el ultimo thread que termina
        logger.info("run: FIN ciclo doJob. isLastWorkingThread()="+isLastWorkingThread());
        if (isLastWorkingThread()) {
        	waitSem.continuar();
        }
	}

	public void doJob() {

		for (Items item : datos) {
			if ("deshabilitar".equalsIgnoreCase(funcion)) {
				String integrationCode = String.format("%d", item.getSku());
				String str = String.format("deshabilitaProductos: local: %s sku: %s integrationCode: %s\n", item.getLocal(), item.getSku(), integrationCode);
				appendFile(str);
				try {
					ProductsClient productsClient = apiClient.getProductsClient();
					Product producto = (Product) productsClient.getByIntegrationCode(integrationCode, Long.valueOf(item.getLocal()));
					if (producto != null) {
						producto.setEnabled(false);
						apiClient.getProductsClient().update(producto, item.getLocal());
					} else
						logger.info(String.format("deshabilitaProductos: No se encuentra integrationCode %s en local %s", item.getSku(), item.getLocal()));
				} catch (Exception e) {
					logger.error(
							String.format("deshabilitaProductos: integrationCode: %s local: %s", integrationCode, item.getLocal()),
							e);
					if (!esErrorFatal(e.getMessage()))
						continue;
					else {
						mustStop = true;
						return;
					}
				}
			}

			if ("habilitar".equalsIgnoreCase(funcion)) {
				String integrationCode = String.format("%d", item.getSku());
				String str = String.format("habilitaProductos: local: %s sku: %s integrationCode: %s\n", item.getLocal(), item.getSku(), integrationCode);
				appendFile(str);
				try {
					ProductsClient productsClient = apiClient.getProductsClient();
					Product producto = (Product) productsClient.getByIntegrationCode(integrationCode, Long.valueOf(item.getLocal()));
					if (producto != null) {
						producto.setEnabled(true);
						apiClient.getProductsClient().update(producto, item.getLocal());
					} else
						logger.info(String.format("habilitaProductos: No se encuentra integrationCode %s en local %s", item.getSku(), item.getLocal()));
				} catch (Exception e) {
					logger.error(
							String.format("habilitaProductos: integrationCode: %s local: %s", integrationCode, item.getLocal()),
							e);
					if (!esErrorFatal(e.getMessage()))
						continue;
					else {
						/*
						mustStop = true;
						return;
						*/
						if ( e instanceof com.pedidosya.reception.sdk.exceptions.ConnectionException) {
							logger.error("Error de comunicacion; espera 10 segs antes de continuar");
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e1) {
								;
							}
						}
						continue;
					}
				}
			}

		}
		try {
			if (fd != null)
				fd.close();
			fd = null;
		} catch (IOException e) {
			;
		}
		working = false;
	}

    /**
     * retorna true si todos los thread de la lista de thraeds tienen su flag
     * working en false
     * @return
     */
    private boolean isLastWorkingThread() {
        for (ThreadStock thread : (ThreadStock[])threads.toArray(new ThreadStock[0])) {
            if(thread.isWorking()) {
            	logger.info("isLastWorkingThread: thread "+((Thread)thread).getName()+" aun trabajando");
                 return false;
            }
        }
        return true;
    }

    private boolean esErrorFatal(String message) {
		String [] nofatales = {
				"restaurant.notExists",
				"product.notExists",
				"security.forbidden"
				};
		for (String msg : nofatales)
			if (message.contains(msg))
				return false;
		return true;
	}

	@Override
	public boolean isWorking() {
		return working;
	}
	private void setWorking(boolean working) {
		this.working = working;
	}

	private void appendFile(String dst) {
		if (fd == null)
			fd = openFile();
		try {
			fd.write(dst);
		} catch (IOException e) {
			logger.error("appendFile", e);
		}
	}

	private BufferedWriter openFile() {
		BufferedWriter fd = null;
		try {
			fd = new BufferedWriter(new FileWriter(flogfile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fd;
	}
}
