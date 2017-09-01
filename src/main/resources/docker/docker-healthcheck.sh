#!/bin/bash
set -eo pipefail

host_ips="$(hostname -i)"
hosts=( $host_ips )

if [[ "$host_ips" =~ .*:.* ]]; then
    ip6_host=$hosts
    if ping="$(ping6 -c 1 "$ip6_host")" && [[ "$ping" =~ .*round-trip.* ]]; then
    exit 0
    fi
else
    ip4_host=$hosts
    if ping="$(ping -c 1 "$ip4_host")" && [[ "$ping" =~ .*round-trip.* ]]; then
    exit 0
    fi
fi
echo "fail"
exit 1
