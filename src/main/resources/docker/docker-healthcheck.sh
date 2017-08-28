#!/bin/bash
set -eo pipefail

host="$(hostname -i || echo '127.0.0.1')"

if ping="$(ping -c 1 "$host")" && [[ "$ping" =~ .*round-trip.* ]]; then
echo "$ping"    
exit 0
fi

exit 1
