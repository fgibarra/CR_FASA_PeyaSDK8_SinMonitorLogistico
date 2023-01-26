#!/bin/sh
# the path to your PID file

# define el periodo en minutos en que se ejecutara el proceso
AMBIENTE_EJECUCION=produccion
PORT=8180

# ============================================================================
# En caso que cambien el clien_id y/o el client_secret
# descomentar:
#client_id=integration_ahumada_2
#client_secret=1|%4c3Ybop
#
# Agregar a la linea donde se activa el proceso (linea 96), que parte con $JAVACMD -jar $SERVICIO .....
# -Dpeya.status.usuario.api=$client_id
# -Dpeya.status.secret.api=$client_secret
#
# ============================================================================

PIDFILE=./pid/peyaStatus.pid
# the path to your  binary, including options if necessary
if [ "x"$JAVA_HOME = "x" ] ; then
#    JAVA_HOME=/usr/java/default
    JAVA_HOME=/usr/java/jdk1.7.0_51
#  JAVA_HOME=/usr/java/jdk1.8.0_181-amd64

fi
JAVACMD=$JAVA_HOME/bin/java
DIR_HOME=/home/jboss/pedidosYa/

BIN_HOME=/home/jboss/pedidosYa/runtime
LOG_HOME=/home/jboss/pedidosYa/runtime/log

SERVICIO=-DPeYAstatus
if [ ""$2 != "" ]; then
	SERVICIO="-D"$2
fi

# A function to find the pid of a program.

pidofproc() {
	ps x | awk 'BEGIN { prog=ARGV[1]; ARGC=1 }
                           { if($7 == prog)
			   {  print $1; exit 0 } }' $1
}

ERROR=0
ARGV="$@"
if [ "x$ARGV" = "x" ] ; then
    ARGS="help"
fi
#definir el CLASSPATH
oldCP=$CLASSPATH

if [ "$oldCP" != "" ]; then
    CLASSPATH=${CLASSPATH}:${oldCP}
fi

for ARG in $@ $ARGS
do
    # check for pidfile
    if [ -f $PIDFILE ] ; then
	PID=`cat $PIDFILE`
	if [ "x$PID" != "x" ] && kill -0 $PID 2>/dev/null ; then
	    STATUS="DPeYAstatus (pid $PID) running"
	    RUNNING=1
	else
	    STATUS="DPeYAstatus (pid $PID?) not running"
	    RUNNING=0
	fi
    else
        PID=`pidofproc $SERVICIO`
        if [ "x$PID" = "x" ]; then
		STATUS="DPeYAstatus not running"
		RUNNING=0
	else
	    STATUS="DPeYAstatus (pid $PID) running"
	    RUNNING=1
	fi
    fi

#echo CLASSPATH $CLASSPATH "servicio=$SERVICIO"

    case $ARG in
    start)
	if [ $RUNNING -eq 1 ]; then
	    echo "$0 $ARG: DPeYAstatus (pid $PID) already running"
	    continue
	fi
	cd $LOG_HOME
        echo $LOG_HOME
        for j in `ls *.log` ; do
                mv $j $j.`date +%y%m%d%k%M%S`
        done
        cd $BIN_HOME
        $JAVACMD -jar $SERVICIO -Xms1024m -Xmx2048m -Dpeya.status.ambiente=$AMBIENTE_EJECUCION -Dpeya.status.port=$PORT PedidosYaStatus.jar &
        echo "$0 $ARG: DPeYAstatus started. cada $PERIODO_EJECUCION minutos"
	;;
    stop)
	if [ $RUNNING -eq 0 ]; then
	    echo "$0 $ARG: $STATUS"
	    continue
	fi
	if kill $PID ; then
	    echo "$0 $ARG: DPeYAstatus stopped"
	else
	    echo "$0 $ARG: DPeYAstatus could not be stopped"
	    ERROR=4
	fi
	;;
    restart)
	if [ $RUNNING -eq 0 ]; then
	    echo "$0 $ARG: DPeYAstatus not running, trying to start"
            $JAVACMD -jar $SERVICIO -Xms1024m -Xmx2048m PedidosYaStatus.jar &
	else
            if kill $PID ; then
                echo "$0 $ARG: DPeYAstatus stopped"
                sleep 30
            else
        	    echo "$0 $ARG: DPeYAstatus could not be stopped"
	            ERROR=4
                    continue
	    fi
  	    cd $LOG_HOME
            for j in `ls *.log` ; do
                mv $j $j.`date +%y%m%d%k%M%S`
            done
	    cd $BIN_HOME
            $JAVACMD -jar $SERVICIO -Xms1024m -Xmx2048m PedidosYaStatus.jar &
            echo "$0 $ARG: DPeYAstatus restarted"
	fi
	;;
    *)
	echo "usage: $0 (start|stop|restart|help)"
	cat <<EOF

start      - start DPeYAstatus
stop       - stop DPeYAstatus
restart    - restart DPeYAstatus if running by sending a SIGHUP or start if
             not running
help       - this screen

EOF
	ERROR=2
    ;;

    esac

done

exit $ERROR

