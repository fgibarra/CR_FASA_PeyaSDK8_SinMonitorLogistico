package module.integracion;

import java.util.Vector;

public class Semaforo {
    private int valor;
    private Vector<Thread> lista;

    ////////////////////////////////////////////////////////////////////////////

    public Semaforo() {
        this(1);
    }

    ////////////////////////////////////////////////////////////////////////////

    public Semaforo(int valor) {
        this.valor = valor;
        lista = new Vector<Thread>();
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
            int actual = valor;

            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        Thread t = (Thread)lista.elementAt(0);
        lista.removeElementAt(0);
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
            notify();
        } else {
            valor++;
        }

    }
}
