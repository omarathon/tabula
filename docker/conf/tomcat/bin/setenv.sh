#!/usr/bin/env bash

#
# The following 2 settings control the minimum and maximum given to the Java virtual machine.
# In larger instances, the maximum amount will need to be increased.
#
JVM_MINIMUM_MEMORY="4g"
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

PRGDIR=`dirname "$0"`

if [ "x$JVM_LIBRARY_PATH" != "x" ]; then
    JVM_LIBRARY_PATH_MINUSD=-Djava.library.path=$JVM_LIBRARY_PATH
    JVM_REQUIRED_ARGS="${JVM_REQUIRED_ARGS} ${JVM_LIBRARY_PATH_MINUSD}"
fi

JAVA_OPTS="-Xms${JVM_MINIMUM_MEMORY} -Xmx${JVM_MAXIMUM_MEMORY} ${JAVA_OPTS} ${JVM_REQUIRED_ARGS}"

JAVA_OPTS="-XX:+UseG1GC -XX:G1HeapRegionSize=32m -Dwarwick.memcached.config=/memcached.properties -Dssoclient.cache.strategy=MemcachedRequired -Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.SLF4JLogger -Dorg.jboss.logging.provider=slf4j ${JAVA_OPTS}"

DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

# Userlookup system properties
USERLOOKUP_SYSTEM_PROPERTIES="-Duserlookup.ssosUrl=https://websignon.warwick.ac.uk/sentry -Duserlookup.groupservice.type=WarwickGroups -Duserlookup.groupservice.location=http://webgroups.warwick.ac.uk -Duserlookup.disk.store.dir=/tmp/ehcache -Dehcache.disk.store.dir=/tmp/ehcache"

JAVA_OPTS="${JAVA_OPTS} ${DEBUG_OPTS} ${JREBEL_SYSTEM_PROPERTIES} ${USERLOOKUP_SYSTEM_PROPERTIES}"

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

#LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:/usr/local/apr/lib"
#export LD_LIBRARY_PATH

