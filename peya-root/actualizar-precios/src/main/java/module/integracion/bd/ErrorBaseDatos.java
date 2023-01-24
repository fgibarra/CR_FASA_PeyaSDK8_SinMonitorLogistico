package module.integracion.bd;

public class ErrorBaseDatos extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1093997771138287358L;
	protected String funcion;

    ///////////////////////////////////////////////////////////////////////////
    public ErrorBaseDatos(String funcion, String msg) {
        super(msg);
        this.funcion = funcion;
    }

    //////////////////////////////////////////////////////////////////////////
    public String getFuncion() {
        return this.funcion;
    }
}
