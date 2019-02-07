#!/bin/bash

PID="$1"
USER="$2"
GROUP="$3"

if [ "$USER" != "UNDEFINED" ]; then
    USER_PART="-u $USER"
else
    USER_PART=""
fi
if [ "$GROUP" != "UNDEFINED" ]; then
    GROUP_PART="-g $GROUP"
else
    GROUP_PART=""
fi

STUBURL=""
#echo "generating stub using jcmd for PID=$PID USER=$USER GROUP=$GROUP ..."
#cho "JAVA_HOME: $JAVA_HOME"
CMD="sudo -n -b ${USER_PART} -E ${GROUP_PART} ${JAVA_HOME}/bin/jcmd ${PID} PerfCounter.print | grep sun.management.JMXConnectorServer.address"
#echo "CMD:$CMD"
STUBURL_KEYVAL=
while [ -z $STUBURL_KEYVAL ]; do
        STUBURL_KEYVAL=$(eval $CMD)
done

$echo "STUBURL_KEYVAL : ${STUBURL_KEYVAL}"
#gets STUBURL by eval of key=val and replacing 'sun.management.JMXConnectorServer.address' with 'STUBURL' below
eval "${STUBURL_KEYVAL/sun.management.JMXConnectorServer.address/STUBURL}"
echo "$STUBURL"

