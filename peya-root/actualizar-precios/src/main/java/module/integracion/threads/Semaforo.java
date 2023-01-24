package module.integracion.threads;

import java.util.Vector;

import org.apache.log4j.Logger;

public class Semaforo {

    private int valor;
    private Logger logWriter = Logger.getLogger(getClass());
    private Vector<Thread> lista;

    ////////////////////////////////////////////////////////////////////////////

    public Semaforo() {
        this(1);
    }

    ////////////////////////////////////////////////////////////////////////////

    public Semaforo(int valor) {
        this.valor = valor;
        lista = new Vector<Thread>();
        logWriter = Logger.getLogger(Semaforo.class);
    }

    ////////////////////////////////////////////////////////////////////////////

    public synchronized void espere() {
        opP();
    }

    public synchronized void permiso() {
        opP();
    }

    ////////////////////////////////////////////////////////////////////////////

    private synchronized void opP() {
        lista.add(Thread.currentThread());
        valor--;

        if (valor < 0) {
            logWriter.debug("opP: espera Thread [" + 
                            Thread.currentThread().getName() + "]. valor=" + 
                            valor);

            try {
                wait();
            } catch (InterruptedException e) {
                logWriter.debug("opP: interrumpido");
            }
        }

        Thread t = (Thread)lista.elementAt(0);
        lista.removeElementAt(0);
        logWriter.debug("opP: continua Thread [" + t.getName() + "]. valor=" + 
                        valor);
    }

    ////////////////////////////////////////////////////////////////////////////

    public synchronized void continuar() {
        opV();
    }

    /////////////////////////////////////////////////////////////////////////////

    private synchronized void opV() {
        Thread esperando;

        if (valor < 0) {
            valor++;
            esperando = (Thread)lista.elementAt(0);

            String name = esperando.getName();
            logWriter.debug("opV: encontro esperando a " + name + " valor=" + 
                            valor);
            notify();
        } else {
            valor++;
        }

        logWriter.debug("opV: Thread [" + Thread.currentThread().getName() + 
                        "]. valor=" + valor);
    }
}
