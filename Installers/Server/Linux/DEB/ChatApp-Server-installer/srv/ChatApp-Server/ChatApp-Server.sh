#!/bin/sh
CHATAPP_SERVER="$(dirname "$0")/bin/ChatAppServer.jar"
VM_ARGUMENTS="-Djava.security.egd=file:/dev/./urandom -server --add-opens java.base/java.lang=ALL-UNNAMED -XX:+UseZGC -Dfile.encoding=UTF-8"

if [ -n "$JAVA_HOME" ]; then
  sudo $JAVA_HOME/bin/java -jar $VM_ARGUMENTS "$CHATAPP_SERVER" "$@"
else
  sudo java -jar $VM_ARGUMENTS "$CHATAPP_SERVER" "$@"
fi
