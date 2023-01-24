package module.integracion.bd;


import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

public class ConnectionPool {

    @SuppressWarnings("unused")
	private String name;
    private String URL;
    private String user;
    private String password;
    private int maxConns;
    private int timeOut;
    protected  Logger logger=Logger.getLogger("montran.lib2.ConnectionPool");
    private int checkedOut;
    private int checkedOutMax;
    private int checkedAcum;
    @SuppressWarnings("unused")
	private int N;
    private int Nacum;
    private int Ncancel;
    private int encoladas;
    private int encoladasMax;
    private int encoladasAcum;
    private int encoladasN;
    @SuppressWarnings("rawtypes")
	private Vector freeConnections = new Vector();
    @SuppressWarnings("rawtypes")
	private Hashtable hashStatus = new Hashtable();

@SuppressWarnings("unused")
private boolean test=false;

    /////////////////////////////////////////////////////////////////////////////
    public ConnectionPool(String name, String URL, String user,
        String password, int maxConns, int initConns, int timeOut,
        PrintWriter pw) throws ErrorBaseDatos {
        this.name = name;
        this.URL = URL;
        this.user = user;
        this.password = password;
        this.maxConns = maxConns;
        this.timeOut = (timeOut > 0) ? timeOut : 5;


        String lf = System.getProperty("line.separator");
        logger.debug(lf + " url=" + URL + lf + " user=" + user + lf +
            " password=" + password + lf + " initconns=" + initConns + lf +
            " maxconns=" + maxConns + lf + " logintimeout=" + this.timeOut);
        logger.debug(getStats());
        initPool(initConns);

        logger.debug("New pool created");
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unchecked")
	private void initPool(int initConns) throws ErrorBaseDatos {
        for (int i = 0; i < initConns; i++) {
            try {
                Connection pc = newConnection();
                freeConnections.addElement(pc);
                hashStatus.put(pc, new ConStatus());
            } catch (SQLException e) {
                logger.error("initPool:" + "errorNumber:" + e.getErrorCode() +
                    "\nSQLState:" + e.getSQLState(), e);
                throw new ErrorBaseDatos("initPool",
                    e.getMessage() + "\nerrorNumber:" + e.getErrorCode() +
                    "\nSQLState:" + e.getSQLState());
            }
        }
    }
    
    /////////////////////////////////////////////////////////////////////////////
    public Connection getConnection() throws SQLException {
        logger.debug("Request for connection received");

        try {
            return getConnection(timeOut * 1000);
        } catch (SQLException e) {
            logger.error( "Exception getting connection", e);
            throw e;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    private synchronized Connection getConnection(long timeout)
        throws SQLException {
        // Get a pooled Connection from the cache or a new one.
        // Wait if all are checked out and the max limit has
        // been reached.
        long startTime = System.currentTimeMillis();
        long remaining = timeout;
        Connection conn = null;
        boolean cuente = true;

        while ((conn = getPooledConnection()) == null) {
            if (cuente) {
                this.encoladas++;

                if (encoladasMax < encoladas) {
                    encoladasMax = encoladas;
                }

                this.encoladasAcum += encoladas;
                this.encoladasN++;
                cuente = false;
            }

            try {
                logger.debug("Waiting for connection. Timeout=" + remaining);
                wait(remaining);
            } catch (InterruptedException e) {
            }

            remaining = timeout - (System.currentTimeMillis() - startTime);

            if (remaining <= 0) {
                // Timeout has expired
                logger.debug("Time-out while waiting for connection");
                throw new SQLException("getConnection() timed-out");
            }
        }

        // Check if the Connection is still OK
        if (!isConnectionOK(conn)) {
            // It was bad. Try again with the remaining timeout
            logger.error("Removed selected bad connection from pool");
            // reactivarla
             hashStatus.remove(conn);
             try {
                 initPool(1);
             }
             catch (ErrorBaseDatos e) {
                 
             }
            return getConnection(remaining);
        }

        if (this.encoladas > 0) {
            encoladas--;
        }

        checkedOut++;

        if (checkedOutMax < checkedOut) {
            checkedOutMax = checkedOut;
        }

        checkedAcum += checkedOut;
        Nacum++;
        logger.debug("Delivered connection from pool");
        logger.debug(getStats());

        return conn;
    }
    /////////////////////////////////////////////////////////////////////////////
    public boolean checkConections() {
        for (int i = 0; i < freeConnections.size(); i++) {
            try {
                Connection conn = getConnection();
                freeConnection(conn);
            }
            catch (SQLException e) {
                logger.debug("checkConections",e);
            }
        }
        return true;
    }
    /////////////////////////////////////////////////////////////////////////////
    private boolean isConnectionOK(Connection conn) {
        Statement testStmt = null;
        ResultSet rs = null;
        try {
           
            if (!conn.isClosed()) {
                // Try to createStatement to see if it's really alive
                
               DatabaseMetaData md = conn.getMetaData();
               String base = md.getDatabaseProductName();
               testStmt = conn.createStatement();
                
                String query="select 1"+(base.toLowerCase().indexOf("oracle")>=0? " from dual":"");
                //String query="select 1 from fasakey where -1=1";
                rs = testStmt.executeQuery(query);
                rs.close();
                testStmt.close();
                
            } else {
                return false;
            }
        } catch (Exception e) {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException se) {
                }
            }
            if (testStmt != null) {
                try {
                    testStmt.close();
                } catch (SQLException se) {
                }
            }
            return false;
        }

        return true;
    }

    /////////////////////////////////////////////////////////////////////////////
    private Connection getPooledConnection() throws SQLException {
        Connection conn = null;

        if (freeConnections.size() > 0) {
            // Pick the first Connection in the Vector
            // to get round-robin usage
            conn = (Connection) freeConnections.firstElement();
            freeConnections.removeElementAt(0);

            ConStatus c = (ConStatus) hashStatus.get(conn);

            if (c != null) {
                c.setStatus(true);
            }
        } else if ((maxConns == 0) || (checkedOut < maxConns)) {
            conn = newConnection();
        }

        return conn;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unchecked")
	private Connection newConnection() throws SQLException {
        Connection conn = null;
        logger.debug("URL=" + URL.toString() + " user=" + user + " password=" +
            password);

        if (user == null) {
            conn = DriverManager.getConnection(URL);
        } else {
            conn = DriverManager.getConnection(URL, user, password);
        }
        
        DatabaseMetaData md = conn.getMetaData();
        String driverName = md.getDriverName();
        N++;
        logger.debug("Opened a new connection with " + driverName);
        hashStatus.put(conn, new ConStatus(true));

        return conn;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unchecked")
	public synchronized void freeConnection(Connection conn) {
        // Put the connection at the end of the Vector
        ConStatus c = (ConStatus) hashStatus.get(conn);

        if (c != null) {
            if (c.getStatus()) {
                freeConnections.addElement(conn);
                c.setStatus(false);
                checkedOut--;
                notifyAll();
                logger.debug("Returned connection to pool");
                logger.debug(getStats());
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings({ "resource", "unchecked" })
	public synchronized void resetConnection(Connection conn) {
        // Put the connection at the end of the Vector
        ConStatus c = (ConStatus) hashStatus.get(conn);

        if (c != null) {
            if (c.getStatus()) {
                this.Ncancel++;

                try {
                    conn.close();
                    logger.debug("Closed connection");
                    hashStatus.remove(conn);

                    if (user == null) {
                        conn = DriverManager.getConnection(URL);
                    } else {
                        conn = DriverManager.getConnection(URL, user, password);
                    }

                    hashStatus.put(conn, new ConStatus());
                    freeConnections.addElement(conn);
                    checkedOut--;
                    notifyAll();
                    logger.debug("Returned connection to pool");
                    logger.debug(getStats());
                } catch (SQLException e) {
                    logger.error( "Couldn't close connection",
                        e);
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    public synchronized void release() {
        @SuppressWarnings("rawtypes")
		Enumeration allConnections = freeConnections.elements();

        while (allConnections.hasMoreElements()) {
            Connection con = (Connection) allConnections.nextElement();

            try {
                con.close();
                logger.debug("Closed connection");
                hashStatus.remove(con);
            } catch (SQLException e) {
                logger.error("Couldn't close connection", e);
            }
        }

        freeConnections.removeAllElements();
    }

    /////////////////////////////////////////////////////////////////////////////
    public String getStats() {
        return "Total de conexiones: " + (freeConnections.size() + checkedOut) +
        " Disponibles: " + freeConnections.size() + " Usandose: " + checkedOut;
    }

    /////////////////////////////////////////////////////////////////////////////
    public int getNumConnections() {
        return checkedOut;
    }

    public int getMaxConnections() {
        return checkedOutMax;
    }

    public int getAvgConnections() {
        return (Nacum > 0) ? Math.round((float) checkedAcum / Nacum) : 0;
    }

    public int getNumEncoladas() {
        return encoladas;
    }

    public int getMaxEncoladas() {
        return this.encoladasMax;
    }

    public int getAvgEncoladas() {
        return (encoladasN > 0)
        ? Math.round((float) encoladasAcum / encoladasN) : 0;
    }

    public int getNumCanceladas() {
        return this.Ncancel;
    }

    public int getFreeConections() {
        return freeConnections.size();
    }

    private class ConStatus {
        private boolean checkedOut;

        public ConStatus() {
            checkedOut = false;
        }

        public ConStatus(boolean valor) {
            checkedOut = valor;
        }

        public void setStatus(boolean valor) {
            checkedOut = valor;
        }

        public boolean getStatus() {
            return checkedOut;
        }
    }
}
