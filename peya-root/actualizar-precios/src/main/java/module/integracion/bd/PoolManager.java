package module.integracion.bd;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

public class PoolManager {

    static private PoolManager instance;
    protected static Logger logger=Logger.getLogger("montran.lib2.PoolManager");
    private PrintWriter pw;
    private Properties dbProps;
    @SuppressWarnings("rawtypes")
	private Vector drivers = new Vector();
    @SuppressWarnings("rawtypes")
	private Hashtable pools = new Hashtable();

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    private PoolManager(Properties dbProps) throws ErrorBaseDatos {
        this.dbProps = dbProps;
        init();
    }

    /////////////////////////////////////////////////////////////////////////////
    static synchronized public PoolManager getInstance()
        throws ErrorBaseDatos {
        return instance;
    }

    /////////////////////////////////////////////////////////////////////////////
    static synchronized public PoolManager getInstance(Properties dbProps)
        throws ErrorBaseDatos {
        if (instance == null) {
            logger.debug("propfile="+dbProps.toString());
            instance = new PoolManager(dbProps);
        }

        return instance;
    }

    /////////////////////////////////////////////////////////////////////////////
    public Connection getConnection(String name) throws ErrorBaseDatos {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            try {
                conn = pool.getConnection();
            } catch (SQLException e) {
                String msg = "Exception al conectar a " + name;
                logger.error( msg, e);
                throw new ErrorBaseDatos("getConnection",
                    msg + " " + e.getMessage());
            }
        }

        return conn;
    }
    @SuppressWarnings("rawtypes")
	synchronized public int geCountFreeConnections(){
        Enumeration en = pools.keys();
        int count = 0;
        while (en.hasMoreElements()) {
            String poolName = (String)en.nextElement();
            count += getFreeConnections(poolName);
        }
        return count;
    }
    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
	synchronized public int getNumConnections(String name) {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getNumConnections();
        }

        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
	synchronized public int getMaxConnections(String name) {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getMaxConnections();
        }

        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
	synchronized public int getAvgConnections(String name) {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getAvgConnections();
        }

        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
	synchronized public int getNumEncoladas(String name) {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getNumEncoladas();
        }

        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
	synchronized public int getMaxEncoladas(String name) {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getMaxEncoladas();
        }

        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
	synchronized public int getAvgEncoladas(String name) {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getAvgEncoladas();
        }

        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
	synchronized public int getNumCanceladas(String name) {
        Connection conn = null;
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getNumCanceladas();
        }

        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    public String getStats(String name) {
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getStats();
        }

        return "";
    }

    /////////////////////////////////////////////////////////////////////////////
    public synchronized int getFreeConnections(String name) {
        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            return pool.getFreeConections();
        }

        return 0;
    }

    /////////////////////////////////////////////////////////////////////////////
    public void freeConnection(String name, Connection con) {
        if (con == null) {
            return;
        }

        try {
            con.commit(); // el sqlserver no vuelve de autocommit false a true

            if (!con.getAutoCommit()) {
                con.setAutoCommit(true);
            }
        } catch (java.sql.SQLException e) {
            logger.debug ("freeConnection",e);
        }

        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            pool.freeConnection(con);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    public void resetConnection(String name, Connection con) {
        if (con == null) {
            return;
        }

        ConnectionPool pool = (ConnectionPool) pools.get(name);

        if (pool != null) {
            pool.resetConnection(con);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("rawtypes")
	public synchronized void checkConections() {
        Enumeration allPools = pools.elements();
        while (allPools.hasMoreElements()) {
            ConnectionPool pool = (ConnectionPool) allPools.nextElement();
            pool.checkConections();
        }
    }
    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("rawtypes")
	public synchronized void release() {
        Enumeration allPools = pools.elements();

        while (allPools.hasMoreElements()) {
            ConnectionPool pool = (ConnectionPool) allPools.nextElement();
            pool.release();
        }

        Enumeration allDrivers = drivers.elements();

        while (allDrivers.hasMoreElements()) {
            Driver driver = (Driver) allDrivers.nextElement();

            try {
                DriverManager.deregisterDriver(driver);
                logger.debug("Deregistrando driver JDBC " +
                    driver.getClass().getName());
            } catch (SQLException e) {
                logger.error("No pudo deregistrar driver JDBC: " +
                    driver.getClass().getName(), e);
            }
        }

        instance = null;
    }

    /////////////////////////////////////////////////////////////////////////////
    private void init() throws ErrorBaseDatos {

        loadDrivers(dbProps);
        createPools(dbProps);
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadDrivers(Properties props) throws ErrorBaseDatos {
        String driverClasses = props.getProperty("drivers");
        StringTokenizer st = new StringTokenizer(driverClasses);
        Vector vecMsg = new Vector();
        boolean procesa = true;
        while (st.hasMoreElements()) {
            String driverClassName = st.nextToken().trim();

            try {
                /*Driver driver = (Driver) Class.forName(driverClassName)
                                              .newInstance();*/
                Class<?> clazz = Class.forName(driverClassName);
                /*
                Constructor<?> ctor = clazz.getConstructor(Driver.class);
                Driver driver = (Driver) ctor.newInstance(new Object[] {});
                DriverManager.registerDriver(driver);
                drivers.addElement(driver);
                */
                vecMsg.add("driver JDBC Registrado " + driverClassName);
            } catch (Exception e) {
                String msg = "No pudo registrar driver JDBC: " + driverClassName+"-"+e.getMessage();
                vecMsg.add(msg);
            	logger.error(msg, e);
                procesa = false;
            }
        }
        if (vecMsg.size() > 0) {
            for (int indx = 0; indx < vecMsg.size(); indx++) {
                String msg = (String)vecMsg.get(indx);
                if (procesa)
                    logger.debug(msg);
                else
                    logger.error(msg);
            }
        }
        if (!procesa) 
            throw new RuntimeException("loadDrivers");
    }

    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void createPools(Properties props) throws ErrorBaseDatos {
        Enumeration propNames = props.propertyNames();

        while (propNames.hasMoreElements()) {
            String name = (String) propNames.nextElement();

            if (name.endsWith(".url")) {
                String poolName = name.substring(0, name.lastIndexOf("."));
                String url = props.getProperty(poolName + ".url");

                if (url == null) {
                    logger.error("No hay URL specificada para " + poolName);

                    continue;
                }

                String user = props.getProperty(poolName + ".user");
                String password = props.getProperty(poolName + ".password");

                String maxConns = props.getProperty(poolName + ".maxconns", "0");
                int max;

                try {
                    max = Integer.valueOf(maxConns).intValue();
                } catch (NumberFormatException e) {
                    logger.error("Valor de maxconns invalido " + maxConns +
                        " para " + poolName);
                    max = 0;
                }

                String initConns = props.getProperty(poolName + ".initconns",
                        "0");
                int init;

                try {
                    init = Integer.valueOf(initConns).intValue();
                } catch (NumberFormatException e) {
                    logger.error("Valor de initconns invalido " + initConns +
                        " para " + poolName);
                    init = 0;
                }

                String loginTimeOut = props.getProperty(poolName +
                        ".logintimeout", "5");
                int timeOut;

                try {
                    timeOut = Integer.valueOf(loginTimeOut).intValue();
                } catch (NumberFormatException e) {
                    logger.error("Valor de logintimeout invalido " + loginTimeOut +
                        " para " + poolName);
                    timeOut = 5;
                }

                ConnectionPool pool = new ConnectionPool(poolName, url, user,
                        password, max, init, timeOut, pw);
                pools.put(poolName, pool);
                
                break;
            }
        }
    }
}
