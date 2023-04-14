package module.integracion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.ApiClient;
import com.pedidosya.reception.sdk.clients.ProductsClient;
import com.pedidosya.reception.sdk.exceptions.ApiException;
import com.pedidosya.reception.sdk.models.Product;
import com.pedidosya.reception.sdk.models.Section;

import cl.ahumada.esb.dto.pedidosYa.consultaStock.json.Items;

public class ThreadStockImpl extends Thread implements ThreadStock {

	private ApiClient apiClient;
	private String funcion;
	private List<Items> datos;
	private Logger logger = Logger.getLogger(getClass());
	private boolean working = false, mustStop = false;
	private List<ThreadStock> threads;
	private Semaforo waitSem;
	private int threadNum;

	public int getThreadNum() {
		return threadNum;
	}

	private File flogfile;
	private BufferedWriter fd = null;
	// key: vendorId, key2: sku
	private Map<Long, Map<Long, Product>> mapdeMaps = new HashMap<Long, Map<Long, Product>>();
	private Map<Long, List<Object>> mapdeSecciones = new HashMap<Long, List<Object>>();

	public ThreadStockImpl(ApiClient apiClient, Semaforo waitSem, List<ThreadStock> threads, String funcion,
			List<Items> datos, int threadNum) {
		super();
		this.apiClient = apiClient;
		this.waitSem = waitSem;
		this.threads = threads;
		this.funcion = funcion;
		this.datos = datos;
		this.threadNum = threadNum;

		// crear archivo para datos
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

		String flogname = String.format("transacciones_%s_%s_%d", funcion, sdf.format(new Date()), threadNum);
		this.flogfile = new File(String.format("%s/log", System.getProperty("user.dir")), flogname);
		logger.info(String.format("archivo contenido trx: %s", flogfile.getAbsolutePath()));

		try {
			// armar estructura de busqueda de productos por sku
			// TODO recuperar lista de secciones para el local
			// buscar el sku entre los productos definidos para el local
			for (Items item : datos) {
				Long vendorId = Long.valueOf(item.getLocal());
				logger.debug(String.format("armar estructura: vendorId: %d", vendorId));
				// verificar si ya no se recupero la lista de seeciones para ese local
				List<Object> listaSecciones = mapdeSecciones.get(vendorId);
				logger.debug(String.format("armar estructura: listaSecciones: %s existe", listaSecciones == null?"NO":"YA"));
				if (listaSecciones == null)
					listaSecciones = apiClient.getSectionClient().getAll(vendorId);
				logger.debug(String.format("armar estructura: listaSecciones: %d items", listaSecciones.size()));
				
				// buscar sku entre los Products asociado al local
				Product product = findProduct(item.getSku(), vendorId, listaSecciones);
				if (product != null) {
					// agregar el sku a la estructura de busqueda
					logger.debug(String.format("armar estructura: Product en PEYA id:%d integrationCode: %s seccion: %s",
							product.getId(), product.getIntegrationCode(), product.getSection().getName()));
					Map<Long, Product> mapSku_producto = mapdeMaps.get(vendorId);
					if (mapSku_producto == null) {
						logger.debug(String.format("armar estructura: nuevo mapSku_producto al local %d", vendorId));
						if (product != null) {
							mapSku_producto = new HashMap<Long, Product>();
							mapSku_producto.put(item.getSku(), product);
							mapdeMaps.put(vendorId, mapSku_producto);
						}
					} else {
						logger.debug(String.format("armar estructura: actualiza mapSku_producto del local %d", vendorId));
						if (mapSku_producto.get(item.getSku()) == null)
							mapSku_producto.put(item.getSku(), product);
					}
				} else {
					logger.error(String.format("SKU %d no se encuentra entre los definidos en PEYA para local %s",
							item.getSku(), item.getLocal()));
				}
			}
			// sacar un dump de la estructura de busqueda
			module.integracion.debug.DumpClass dc = new module.integracion.debug.DumpClass(mapdeMaps);
			logger.info(String.format("Estructura de busqueda para set de datos en thread %s:\n%s",
					Thread.currentThread().getName(), dc.toString()));
			
		} catch (ApiException e) {
			logger.error("Armando estructura de busqueda de productos por sku", e);
		}
	}

	private Product findProduct(Long sku, Long vendorId, List<Object> listaSecciones) throws ApiException {
		for (Object obj : listaSecciones) {
			Section section = (Section) obj;
			Product menuItem = new Product();
			menuItem.setIntegrationCode(sku.toString());
			menuItem.setSection(section);
			List<Object> listaProductos = apiClient.getProductsClient().getAll(menuItem, vendorId);

			if (listaProductos != null && listaProductos.size() > 0) {
				for (Object objProd : listaProductos) {
					Product product = (Product) objProd;
					Long skuProduct = Long.valueOf(product.getIntegrationCode());
					if (skuProduct == sku)
						return product;
				}
			}
		}
		return null;
	}

	public void run() {
		working = true;
		do {
			doJob();
			if (mustStop)
				setWorking(false);
		} while (isWorking());
		// determinar si es el ultimo thread que termina
		logger.info("run: FIN ciclo doJob. isLastWorkingThread()=" + isLastWorkingThread());
		if (isLastWorkingThread()) {
			waitSem.continuar();
		}
	}

	public void doJob() {

		for (Items item : datos) {
			if ("deshabilitar".equalsIgnoreCase(funcion)) {
				String integrationCode = String.format("%d", item.getSku());
				String str = String.format("deshabilitaProductos: local: %s sku: %s integrationCode: %s\n",
						item.getLocal(), item.getSku(), integrationCode);
				appendFile(str);
				try {
					ProductsClient productsClient = apiClient.getProductsClient();
					Product producto = buscarProducto(item);
					/*
					 * No funciona en sdk8 Product producto = (Product)
					 * productsClient.getByIntegrationCode(integrationCode,
					 * Long.valueOf(item.getLocal()));
					 */
					if (producto != null) {
						producto.setEnabled(false);
						apiClient.getProductsClient().update(producto, item.getLocal());
					} else
						logger.info(
								String.format("deshabilitaProductos: No se encuentra integrationCode %s en local %s",
										item.getSku(), item.getLocal()));
				} catch (Exception e) {
					logger.error(String.format("deshabilitaProductos: integrationCode: %s local: %s", integrationCode,
							item.getLocal()), e);
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
				String str = String.format("habilitaProductos: local: %s sku: %s integrationCode: %s\n",
						item.getLocal(), item.getSku(), integrationCode);
				appendFile(str);
				try {
					ProductsClient productsClient = apiClient.getProductsClient();
					Product producto = buscarProducto(item);

					/*
					 * No funciona en sdk8 Product producto = (Product)
					 * productsClient.getByIntegrationCode(integrationCode,
					 * Long.valueOf(item.getLocal()));
					 */
					if (producto != null) {
						producto.setEnabled(true);
						apiClient.getProductsClient().update(producto, item.getLocal());
					} else
						logger.info(String.format("habilitaProductos: No se encuentra integrationCode %s en local %s",
								item.getSku(), item.getLocal()));
				} catch (Exception e) {
					logger.error(String.format("habilitaProductos: integrationCode: %s local: %s", integrationCode,
							item.getLocal()), e);
					if (!esErrorFatal(e.getMessage()))
						continue;
					else {
						/*
						 * mustStop = true; return;
						 */
						if (e instanceof com.pedidosya.reception.sdk.exceptions.ConnectionException) {
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

	private Product buscarProducto(Items item) {
		// Arma objeto producto con integrationCode y Section
		Long integrationCode = item.getSku();
		Long vendorId = Long.valueOf(item.getLocal());
		Map<Long, Product> mapSkuxProducto = mapdeMaps.get(vendorId);
		Product product = mapSkuxProducto.get(integrationCode);
		return product;
	}

	/**
	 * retorna true si todos los thread de la lista de thraeds tienen su flag
	 * working en false
	 * 
	 * @return
	 */
	private boolean isLastWorkingThread() {
		for (ThreadStock thread : (ThreadStock[]) threads.toArray(new ThreadStock[0])) {
			if (thread.isWorking()) {
				logger.info("isLastWorkingThread: thread " + ((Thread) thread).getName() + " aun trabajando");
				return false;
			}
		}
		return true;
	}

	private boolean esErrorFatal(String message) {
		String[] nofatales = { "restaurant.notExists", "product.notExists", "security.forbidden" };
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
