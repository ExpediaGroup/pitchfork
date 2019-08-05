#!/bin/bash

[ -z "$JAVA_XMS" ] && JAVA_XMS=1024m
[ -z "$JAVA_XMX" ] && JAVA_XMX=1024m
[ -z "$JAVA_GC_OPTS" ] && JAVA_GC_OPTS="-XX:+UseG1GC"

set -e
JAVA_OPTS="${JAVA_OPTS} \
-javaagent:/pitchfork/${JMXTRANS_AGENT}.jar=/pitchfork/jmxtrans-agent.xml \
${JAVA_GC_OPTS} \
-Xmx${JAVA_XMX} \
-Xms${JAVA_XMS} \
-XX:+ExitOnOutOfMemoryError \
-Dapplication.name=pitchfork \
-Dapplication.home=/pitchfork"

if [[ -n "$SERVICE_DEBUG_ON" ]] && [[ "$SERVICE_DEBUG_ON" == true ]]; then
   JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y"
fi

exec /opt/jdk-mini/bin/java ${JAVA_OPTS} -jar pitchfork.jar

#-javaagent:/pitchfork/jmxtrans-agent-1.2.6.jar=/pitchfork/jmxtrans-agent.xml -XX:+UseG1GC -Xmx1024m -Xms1024m -XX:+ExitOnOutOfMemoryError -Dapplication.name=pitchfork -Dapplication.home=/pitchfork

#CMD export TZ=$(date +%Z) &&\
#    exec $JAVA_HOME/bin/java \
#         ${JAVA_OPTS} \
#         -jar \
#         pitchfork.jar
