#!/bin/bash

## This script starts three independent PHP web servers. These are needed to
## accept multiple requests to different endpoints (the PHP server only accepts
## one request at a time).


HOST="localhost"

# Change the ports to any available ports greater than 1024 in your system.
CORE_PORT=9800
SVC_PORT=9801
REPO_PORT=9802

# Directory where the temporary pid files are written.
PIDFILE_DIR="/tmp"
CORE_PIDFILE="${PIDFILE_DIR}/apix-core.pid"
SVC_PIDFILE="${PIDFILE_DIR}/apix-service.pid"
REPO_PIDFILE="${PIDFILE_DIR}/apix-repo.pid"

CORE_SCRIPT="./core.php"
SVC_SCRIPT="./services/validation.php"
REPO_SCRIPT="./services/fcrepo.php"

# These env variables are used by the core script to call the other services.
#export CORE_ENDPOINT="http://${HOST}:${CORE_PORT}"
export SVC_ENDPOINT="http://${HOST}:${SVC_PORT}"
export REPO_ENDPOINT="http://${HOST}:${REPO_PORT}"
export TMPDIR="/tmp"

if [ "$1" == "start" ]; then
    php -S $HOST:$CORE_PORT -t $(dirname $(realpath $CORE_SCRIPT)) $CORE_SCRIPT &
    echo $! >| $CORE_PIDFILE

    php -S $HOST:$SVC_PORT -t $(dirname $(realpath $SVC_SCRIPT)) $SVC_SCRIPT &
    echo $! >| $SVC_PIDFILE

    php -S $HOST:$REPO_PORT -t $(dirname $(realpath $REPO_SCRIPT)) $REPO_SCRIPT &
    echo $! >| $REPO_PIDFILE
fi

if [ "$1" == "stop" ]; then
    kill `cat $CORE_PIDFILE`
    kill `cat $SVC_PIDFILE`
    kill `cat $REPO_PIDFILE`
fi

