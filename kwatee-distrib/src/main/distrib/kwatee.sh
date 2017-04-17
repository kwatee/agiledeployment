#!/bin/sh
#
# chkconfig: 234 65 33
# description: kwatee stop start restart status
### BEGIN INIT INFO
# Provides: kwatee
### END INIT INFO

# If JAVA_HOME is not defined in your environment, define it below
#JAVA_HOME=
JAVA_OPTS="-Xmx1024m -XX:PermSize=64m -XX:MaxPermSize=256m"

if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME is undefined"
  exit -1
fi

if [ ! -f $JAVA_HOME/bin/java ]; then
  echo "$JAVA_HOME/bin/java NOT FOUND"
  exit -1
fi

# Absolute path to this script
SCRIPT=$(readlink "$0")
# Absolute path this script is in
KWATEE_HOME=$(dirname "$SCRIPT")

KWATEE_PIDFILE=$KWATEE_HOME/kwatee.pid

case "$1" in

"")
   cd $KWATEE_HOME && $JAVA_HOME/bin/java $JAVA_OPTS -jar kwatee-${project.version}.jar
   ;;
"debug")
   cd $KWATEE_HOME && $JAVA_HOME/bin/java $JAVA_OPTS -Dlogging.level.root=DEBUG -jar kwatee-${project.version}.jar
   ;;
"start")
   if [ -s "$KWATEE_PIDFILE" ]; then
      echo "$KWATEE_PIDFILE already exists"
      exit 0
   fi
   cd $KWATEE_HOME && $JAVA_HOME/bin/java $JAVA_OPTS -Dpidfile=$KWATEE_PIDFILE -jar kwatee-${project.version}.jar >/dev/null 2>&1 &
   echo "OK - starting..."
   ;;
"stop")
   if [ -s "$KWATEE_PIDFILE" ]; then
      PID=$(cat "$KWATEE_PIDFILE")
      echo "stopping..."
      kill -15 $PID
      sleep 2
      kill -0 $PID >/dev/null 2>&1
      if [ $? == 0 ]; then
         kill -9 $PID >/dev/null 2>&1
         sleep 1
         kill -0 $PID >/dev/null 2>&1
         if [ $? == 0 ]; then
            echo "CRITICAL - impossible to kill process $PID"
            exit 2
         fi
      fi
      rm -f $kWATEE_PIDFILE
   fi
   echo "OK - stopped"
   ;;
"restart")
   if [ -s "$KWATEE_PIDFILE" ]; then
      PID=$(cat "$KWATEE_PIDFILE")
      echo "stopping..."
      kill -15 $PID
      sleep 2
      kill -0 $PID >/dev/null 2>&1
      if [ $? == 0 ]; then
         kill -9 $PID >/dev/null 2>&1
         sleep 1
         kill -0 $PID >/dev/null 2>&1
         if [ $? == 0 ]; then
            echo "CRITICAL - impossible to kill process $PID"
            exit 2
         fi
      fi
      rm -f $kWATEE_PIDFILE
   fi
   echo "OK - stopped"
   cd $KWATEE_HOME && $JAVA_HOME/bin/java $JAVA_OPTS -Dpidfile=$KWATEE_PIDFILE -jar kwatee-${project.version}.jar >/dev/null 2>&1 &
   echo "OK - starting..."
   ;;
"status")
   if [ -s "$KWATEE_PIDFILE" ]; then
      PID=$(cat "$KWATEE_PIDFILE")
      kill -0 $PID >/dev/null 2>&1
      if [ $? == 0 ]; then
         echo "OK - Kwatee($PID) is running"
      else
         echo "CRITICAL - $KWATEE_PIDFILE exists but process $PID is unreachable"
         exit 2 
      fi
   else
     echo "CRITICAL - not running"
     exit 2
   fi
   ;;
"cli")
   cd $KWATEE_HOME && $JAVA_HOME/bin/java $JAVA_OPTS -Dloader.main=net.kwatee.agiledeployment.application.cli.CLIApplication -jar kwatee-${project.version}.jar $2 $3 $4 $5 $6 $7 $8 $9
   ;;
*)
   echo "Unknown option: $1"
   echo "Usage: $0 [start|stop|restart|status]"
   ;;
esac
