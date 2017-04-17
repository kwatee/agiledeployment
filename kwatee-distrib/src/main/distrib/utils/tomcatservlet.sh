#!/bin/sh
# kwatee simple servlet start/stop/status utility
#

TOMCAT_SERVLET_CTL=curl --user %{TOMCAT_USER}:%{TOMCAT_PASSWORD} http://localhost:%{TOMCAT_PORT}/manager/test

#
# This starts the servlet using the 
# return : 0 on success or if already running
#          3 if already running
#
do_start () {
$TOMCAT_SERVLET_CTL/start/?path=/$1 | grep "OK - Started"
if [ $? -eq 0 ] ; then
    echo "[kwatee_running] servlet is active"
    exit 0
fi
echo "[kwatee_error]"
exit 0
}


#
# This stops the process
# return : 0 on success
#
do_stop() {
$TOMCAT_SERVLET_CTL/stop/?path=/$1 | grep "OK - Stopped"
if [ $? -eq 0 ] ; then
    echo "[kwatee_stopped] servlet is stopped"
    exit 0
fi
echo "[kwatee_error]"
exit 0
}


#
# This retrieves the status
# return : always 0
#
do_status () {
$TOMCAT_SERVLET_CTL/sessions/?path=/$1 | grep "OK - Sessions"
if [ $? -eq 0 ] ; then
    echo "[kwatee_running] servlet is active"
    exit 0
fi
echo "[kwatee_stopped] server is stopped"
exit 0
}




###################################################
# Main
###################################################


#
# Parse command line parameters
#
if [ -z "$2" ] ; then
echo missing servlet parameter
exit 22
fi

case "$1" in
start)
do_start $2
RETVAL=$?
;;
status)
do_status $2
exit 0
;;
stop)
do_stop $2
exit 0
;;
*)
echo "[kwatee_error] usage : $0 {start <servlet> | stop <servlet> | status <servlet>}"
exit 22
esac

if [ $RETVAL -eq 0 ] ; then
echo "[kwatee_ok]"
elif [ $RETVAL -eq 130 ] ; then
echo "[kwatee_error] interrupted"
else
echo "[kwatee_error] failed"
fi

exit $RETVAL
