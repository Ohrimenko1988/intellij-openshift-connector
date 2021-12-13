#!/bin/sh

# inject env. variables from env properties file
set -a
. ${WORKSPACE}/local_env.properties
set +a

crc_started() {
    openshiftStatus=$(./crc status | grep OpenShift | awk -F: '{ print $2 }' | tr -d " ")
    crcStatus=$(./crc status | grep "CRC VM:" | awk -F: '{ print $2 }' | tr -d " ")
    result=0
    if [[ $openshiftStatus == *Running* ]] && [[ $crcStatus == *Running* ]]; then
        result=1
    fi
    echo $result
}

cd ${WORKSPACE2}/crc

## Start crc and install all necessary stuff

./${BASEFILE_NAME} start -p ${CRC_PULL_SECRET} --memory 18432 --cpus 6

./${BASEFILE_NAME} status || true

# verify crc is started
echo "CRC is starting..."
treshold=240
timer=0
starting_result="CRC is started and ready"
while [ "$(crc_started)" != "1" ]; do
    echo "waiting for $timer sec..."
    sleep 10
    ((timer=timer+10))
    if [ $timer -ge $treshold ]; then
        starting_result="Timeout reached when starting CRC"
        echo $starting_result
        exit 1
    fi
done

echo $starting_result

# add oc to the path
./${BASEFILE_NAME} oc-env
eval $(./${BASEFILE_NAME} oc-env)

oc version

# log into a cluster using oc
# password can be obtained from ~/.crc/cache/crc_libvirt_4.5.1/kubeadmin-password
# 4.5.1 string can be get from ./crc version or from client version: oc version
pass=`cat ~/.crc/cache/crc_libvirt_$(oc version | head -n 1 | cut -d':' -f2 | xargs)/kubeadmin-password`
oc login -u kubeadmin -p ${pass} https://api.crc.testing:6443 --insecure-skip-tls-verify

