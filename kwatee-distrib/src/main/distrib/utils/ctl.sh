#!/bin/sh
# This sample control script may be adjusted to your specific requirements
#

MODULE_USER_OWNER=%{MODULE_USER_OWNER}
MODULE_USER_GROUP=%{MODULE_USER_GROUP}
MODULE_NAME=%{MODULE_NAME}
MODULE_DIR=`pwd`
PIDFILE=/var/log/$MODULE_NAME/$MODULE_NAME.pid

#
# This sets up artifact-related elements that are not included in the artifact's package 
#
module_setup () {
	# In case the process was running, stop it
	module_stop()
	echo "Create log directory"
	mkdir /var/log/$MODULE_NAME
	chown $MODULE_USER_OWNER:$MODULE_USER_GROUP /var/log/$MODULE_NAME
	echo "Create pid directory"
	mkdir /var/run/$MODULE_NAME
	chown $MODULE_USER_OWNER:$MODULE_USER_GROUP /var/run/$MODULE_NAME
    chmod +x $MODULE_DIR/ctl.sh
    # Launch on startup
    ln -s /etc/init.d/$MODULE_NAME $MODULE_DIR/ctl.sh
    chkconfig ...
    echo "[kwatee_ok] setup completed"
exit 0
}

#
# This removes artifact-related elements that were added in the setup phase
#
module_teardown () {
	module_stop()
	echo "Deleting log directory"
	rm -rf /var/log/$MODULE_NAME
	echo "Deleting pid directory"
	rm -rf /var/run/$MODULE_NAME
    chkconfig ...
    rm -f /etc/init.d/$MODULE_NAME
    echo "[kwatee_ok] teardown completed"
exit 0
}


#
# This starts the module
# return : 0 on success or if already running
#          3 if already running
#
module_start () {
	if [ -f $PIDFILE ] ; then
		echo "Found pid file"
		PID = `cat $PIDFILE`
		if $PID ; then
			echo "[kwatee_running] already running"
			exit 0
		fi
		rm -f $PIDFILE
	fi
	CURRENT_USER=`whoami`
	if [ $CURRENT_USER -neq $MODULE_USER_OWNER ] ; then
		$START_MODULE
	else
		su - $MODULE_USER_OWNER -c "$START_MODULE"
	fi
	echo $! > $PIDFILE
echo "[kwatee_running]"
exit 0
}


#
# This stops the process
# return : 0 on success
#
module_stop() {
	if [ -f $PIDFILE ] ; then
		echo "Found pid file"
		PID = `cat $PIDFILE`
		if $PID ; then
			echo "Send kill -15"
			kill -15 $PID
			sleep 3
			if $PID ; then
				echo "Send kill -9"
				kill -9 $PID
			fi
		fi
		rm -f $PIDFILE
	fi
	echo "[kwatee_stopped]"
exit 0
}


#
# This retrieves the status
# return : always 0
#
do_status () {
	if [ -f $PIDFILE ] ; then
		echo "Found pid file"
		PID = `cat $PIDFILE`
		if $PID ; then
		    echo "[kwatee_running]"
		    exit 0
		fi
		rm -f $PIDFILE
	fi
	echo "[kwatee_stopped]"
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
setup)
do_setup
;;
teardown)
do_teardown
;;
start)
do_start
;;
status)
do_status
;;
stop)
do_stop
;;
*)
echo "[kwatee_error] usage : $0 {setup | teardown | start | stop | status}"
exit 22
esac
exit 0