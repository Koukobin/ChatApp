#!/bin/sh
CHATAPP_CLIENT="$(dirname "$0")/bin/ErmisClient.jar"
VM_ARGUMENTS="-client --add-opens java.base/java.lang=ALL-UNNAMED -XX:+UseZGC -XX:+ShrinkHeapInSteps -XX:MinHeapFreeRatio=2 -XX:MaxHeapFreeRatio=5 -Dfile.encoding=UTF-8"

/opt/Ermis-Client/jre/zulu17.40.19-ca-jre17.0.6-linux_x64/bin/java $VM_ARGUMENTS -jar "$CHATAPP_CLIENT"
