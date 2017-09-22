#!/usr/bin/env bash
if [ "x${JAVA_HOME}" = "x" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-8-oracle
fi

#
# Native libraries, such as the Tomcat native library, can be placed here.

# NOTE: You must choose the library architecture, x86 or x64, based on the JVM you'll be running, _not_ based on the OS.
#
#JVM_LIBRARY_PATH="$CATALINA_HOME/lib/native"
JVM_LIBRARY_PATH="/usr/local/apr/lib"

#
# The following 2 settings control the minimum and maximum given to the Java virtual machine.
# In larger instances, the maximum amount will need to be increased.
#
JVM_MINIMUM_MEMORY="1g"
JVM_MAXIMUM_MEMORY="4g"

#
# File encoding passed into the Java virtual machine
#
# Deliberately uses a rubbish encoding to make sure we're always setting UTF-8 in strings
#
JVM_FILE_ENCODING="ISO646-US"

#
# The following are the required arguments needed.
#
JVM_REQUIRED_ARGS="-Djava.awt.headless=true -Dfile.encoding=${JVM_FILE_ENCODING} -Dorg.apache.jasper.runtime.BodyContentImpl.LIMIT_BUFFER=true -Dmail.mime.decodeparameters=true -Dorg.apache.catalina.connector.Response.ENFORCE_ENCODING_IN_GET_WRITER=false"

#-----------------------------------------------------------------------------------
# JMX
#
# JMX is enabled by selecting an authentication method value for JMX_REMOTE_AUTH and then configuring related the
# variables.
#
# See http://docs.oracle.com/javase/7/docs/technotes/guides/management/agent.html for more information on JMX
# configuration in general.
#-----------------------------------------------------------------------------------

#
# Set the authentication to use for remote JMX access. Anything other than "password" will cause remote JMX
# access to be disabled.
#
JMX_REMOTE_AUTH=

#
# The port for remote JMX support if enabled
#
#JMX_REMOTE_PORT=8086

#
# If `hostname -i` returns a local address then JMX-RMI communication may fail because the address returned by JMX for
# the RMI-JMX stub will not resolve for non-local clients. To fix this you will need to explicitly specify the
# IP address / host name of this server that is reachable / resolvable by JMX clients. e.g.
# RMI_SERVER_HOSTNAME="-Djava.rmi.server.hostname=non.local.name.of.my.server"
#
#RMI_SERVER_HOSTNAME="-Djava.rmi.server.hostname="

#-----------------------------------------------------------------------------------
# JMX username/password support
#-----------------------------------------------------------------------------------

#
# The full path to the JMX username/password file used to authenticate remote JMX clients
#
#JMX_PASSWORD_FILE=/var/tomcat/servers/tabula/bin/jmx-users.password

PRGDIR=`dirname "$0"`

if [ "x$JMX_REMOTE_AUTH" = "xpassword" ]; then
    if [ -z "$JMX_PASSWORD_FILE" ] || [ ! -f "$JMX_PASSWORD_FILE" ]; then
        echo ""
        echo "-------------------------------------------------------------------------------"
        echo "  Remote JMX with username/password authentication is enabled.                 "
        echo "                                                                               "
        echo "  You must specify a valid path to the password file used by Stash.            "
        echo "  This is done by specifying JMX_PASSWORD_FILE in setenv.sh.                   "
        echo "-------------------------------------------------------------------------------"
        exit 1
    fi

    JMX_OPTS="-Dcom.sun.management.jmxremote.port=${JMX_REMOTE_PORT} ${RMI_SERVER_HOSTNAME} -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.password.file=${JMX_PASSWORD_FILE}"
fi

if [ "x$JVM_LIBRARY_PATH" != "x" ]; then
    JVM_LIBRARY_PATH_MINUSD=-Djava.library.path=$JVM_LIBRARY_PATH
    JVM_REQUIRED_ARGS="${JVM_REQUIRED_ARGS} ${JVM_LIBRARY_PATH_MINUSD}"
fi

JAVA_OPTS="-Xms${JVM_MINIMUM_MEMORY} -Xmx${JVM_MAXIMUM_MEMORY} ${JMX_OPTS} ${JAVA_OPTS} ${JVM_REQUIRED_ARGS}"

JAVA_OPTS="-XX:+UseG1GC -XX:G1HeapRegionSize=32m ${JAVA_OPTS}"

DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

# System properties used for JRebel
JREBEL_SYSTEM_PROPERTIES="-Drebel.workspace.path=/home/mat/workspace"

# General JRebel config
JREBEL_CONFIG="-Drebel.log=true -Drebel.log.request=true -agentpath:/opt/jrebel/lib/libjrebel64.so -Drebel.always.rerun.clinit=true"

# XRebel config
#XREBEL_CONFIG="-javaagent:/opt/xrebel/xrebel.jar"

# Userlookup system properties
USERLOOKUP_SYSTEM_PROPERTIES="-Duserlookup.ssosUrl=https://websignon.warwick.ac.uk/sentry -Duserlookup.groupservice.type=WarwickGroups -Duserlookup.groupservice.location=http://webgroups.warwick.ac.uk -Duserlookup.disk.store.dir=/tmp/ehcache -Dehcache.disk.store.dir=/tmp/ehcache"

JAVA_OPTS="${JAVA_OPTS} ${DEBUG_OPTS} ${JREBEL_SYSTEM_PROPERTIES} ${JREBEL_CONFIG} ${XREBEL_CONFIG} ${USERLOOKUP_SYSTEM_PROPERTIES}"

export JAVA_OPTS

# set the location of the pid file
if [ -z "$CATALINA_PID" ] ; then
    if [ -n "$CATALINA_BASE" ] ; then
        CATALINA_PID="$CATALINA_BASE"/work/catalina.pid
    elif [ -n "$CATALINA_HOME" ] ; then
        CATALINA_PID="$CATALINA_HOME"/work/catalina.pid
    fi
fi
export CATALINA_PID

LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:/usr/local/apr/lib"
export LD_LIBRARY_PATH