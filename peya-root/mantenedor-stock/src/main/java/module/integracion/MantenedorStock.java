package module.integracion;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.pedidosya.reception.sdk.ApiClient;

import cl.ahumada.esb.dto.pedidosYa.consultaStock.json.Items;

public class MantenedorStock extends Thread {

	private ApiClient apiClient;
	private Logger logger = Logger.getLogger(getClass());
	private ServiciosdeBus serviciosdeBus;
	int NUM_THREADS = 10;
    private Semaforo waitSem = new Semaforo(0);
    private List<ThreadStock> threads;
	int countPasadas = 0;
	boolean conDump = true;
	private Long periodo;

	public MantenedorStock(ApiClient apiClient) throws IOException {
		this.apiClient = apiClient;
		serviciosdeBus = new ServiciosdeBus(apiClient);
		threads = new ArrayList<ThreadStock>();
		String valor = serviciosdeBus.getIntegracionProps().getProperty("stock_num_threads");
		if (valor != null)
			try {
				NUM_THREADS = Integer.valueOf(valor);
			} catch (Exception e) {
				logger.error(String.format("Valor de propiedad %s no es numerico, asume %d", valor, NUM_THREADS));
			}
		valor = System.getProperty("peya.periodo.stock");
		if (valor == null)
			valor = serviciosdeBus.getIntegracionProps().getProperty("peya.periodo.stock");
		logger.info(String.format("MantenedorStock: peya.periodo.stock=%s", valor));
		periodo = valor != null ? Long.valueOf(valor) * 60000l : 3600000l;
	}

	public void actualizaStock() {

		// corre
		final TimerTask task;
		synchronized (this) {
			task = new TimerTask() {
				@Override
				public void run() {
					int total = 0;
					logger.info(String.format("actualizaStock: parte %s", getHoraPartida()));
					try {
						// pide los sku para deshabilitar
						Items[] deshabilitar = serviciosdeBus.getItemsDeshabilitar();
						if (deshabilitar != null && deshabilitar.length > 0) {
							if (conDump)
								dumpEntrada(deshabilitar, "El sp entrega para deshabilitar");
							int count = procesa("deshabilitar", Arrays.asList(deshabilitar));
							total += count;
							logger.info(String.format("actualizaStock: termina deshabilitados %s informados: %d", getHoraPartida(), count));
						} else
							logger.info(String.format("actualizaStock: NO recupera datos para deshabilitados %s", getHoraPartida()));
						// pide los sku para habilitar
						Items[] habilitar = serviciosdeBus.getItemsHabilitar();
						if (conDump)
							dumpEntrada(deshabilitar, "El sp entrega para habilitar");
						if (habilitar != null && habilitar.length > 0) {
							int count = procesa("habilitar", Arrays.asList(habilitar));
							total += count;
							logger.info(String.format("actualizaStock: termina habilitados %s informados: %d", getHoraPartida(), count));
						} else
							logger.info(String.format("actualizaStock: NO recupera datos para habilitados %s", getHoraPartida()));


					} catch (Exception ex) {
						logger.error("Error in actualizaStock", ex);
					}
					logger.info(String.format("actualizaStock: termina ambos %s total informado: %d", getHoraPartida(), total));
				}

			};

			logger.info(String.format("actualizaStock: genera timer pasadas: %d", countPasadas++));
			new Timer().schedule(task, 10000, periodo);
		}
	}

	public int procesa(String funcion, List<Items> todos) {

		int numElementos = todos.size() / NUM_THREADS;
		@SuppressWarnings("unchecked")
		List<Items> datos[] = new List[NUM_THREADS];
		int index=0;
		threads.clear();

		for (int i = 0; i<datos.length; i++) {
			datos[i] = new ArrayList<Items>();
			for (int j=0; j<numElementos; j++)
				datos[i].add(todos.get(index++));
		}
		// repartir el resto entre los threads que alcancen
		for (int i = 0; i<datos.length && index < todos.size(); i++) {
			datos[i].add(todos.get(index++));
		}

		if (conDump) {
			int i=1;
			for (List<Items> lista : datos) {
				logger.info(String.format("datos a procesar por thread %d", i));
				dumpEntrada(lista.toArray(new Items[0]),String.format("%s thread %d", funcion, i));
				i++;
			}
		}
		// engendrar los thread que procesen
		for (int i=0; i<NUM_THREADS; i++) {
			if (datos[i].size() > 0) {
				ThreadStockImpl thread  = new ThreadStockImpl(apiClient, waitSem, threads, funcion, datos[i], i);

	            if (thread != null) {
	                threads.add(thread);
	                thread.start();
	            }
			}
		}
		// esperar a que terminen
		waitSem.espere();

		return index;
	}

	private void dumpEntrada(Items[] deshabilitar, String operacion) {
		if (deshabilitar != null && deshabilitar.length > 0)
			for (Items item : deshabilitar) {
				logger.info(String.format("%s: local:%s sku:%s", operacion, item.getLocal(), item.getSku()));
			}
		else
			logger.info(String.format("No vienen items para operacion %s", operacion));
	}


	public String getHoraPartida() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return sdf.format(new Date());
	}
}
