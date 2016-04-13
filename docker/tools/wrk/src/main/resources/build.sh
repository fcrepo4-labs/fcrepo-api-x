#!/bin/sh
export WRK_DIR=${WRK_DIR=/tmp/wrk}
export BUILD_ROOT=${BUILD_ROOT=/tmp/build}
export WRK_VERSION=${WRK_VERSION=4.0.2}

echo "build root" ${BULD_ROOT}
mkdir -p ${BUILD_ROOT}
mkdir -p ${WRK_DIR}

if [ ! -f /tmp/build/wrk ]; then
    cd ${BUILD_ROOT}
    git clone https://github.com/wg/wrk.git
    cd ${BUILD_ROOT}/wrk
    git checkout ${WRK_VERSION}
fi

cd ${BUILD_ROOT}/wrk
git checkout ${WRK_VERSION}

if [ -f /sbin/apk ]; then
    echo "apline"
    # If alpine, make a static binary usable on any Linux
    apk add --no-cache luajit-dev openssl-dev
    find . -name '*.?' -type f -exec sed -i s/zcalloc/zcalloc_/g {} +
    #cd src; patch < ${WRK_DIR}/static.patch; cd ..
    make LIBS="-lpthread -lm -lssl -lcrypto -lz -lluajit-5.1" LDFLAGS="-static" WITH_LUAJIT=/usr WITH_OPENSSL=/usr
else
    # Just make via standard build
    echo "no alpine"
    make
fi

cp ${BUILD_ROOT}/wrk/wrk ${WRK_DIR}
