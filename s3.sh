#!/bin/bash

function putS3
{
    objectName=$1
    file=$objectName
    bucket=apixtravis
    resource="/${bucket}/buildoutput/${objectName}"
    contentType="application/octet-stream"
    dateValue=$(date +"%a, %d %b %Y %T %z")
    stringToSign="PUT\n\n${contentType}\n${dateValue}\n${resource}"
    s3Key=AKIAI2HKU45U4Q6M3YDA
    s3Secret=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
    signature=`echo -en ${stringToSign} | openssl sha1 -hmac ${s3Secret} -binary | base64`
    echo "Putting ${file} to https://${bucket}.s3.amazonaws.com/${objectName}"
    curl -X PUT -T "${file}" \
              -H "Host: ${bucket}.s3.amazonaws.com" \
              -H "Date: ${dateValue}" \
              -H "Content-Type: ${contentType}" \
              -H "Authorization: AWS ${s3Key}:${signature}" \
              https://${bucket}.s3.amazonaws.com/${objectName}
}

for file in `find fcrepo-api-x-integration/target` ;
do
    putS3 $file
done
