#! /bin/bash
#####
# This file is used to deploy war through wildfly web API
#####

# Configuration
SERVER_NAME="01-Server"
JBOSS_ADDR="http://admin:password@hostname:ports"

echo "Deploy Something to the server: "${SERVER_NAME}
echo "-----------------------------------"
echo "Step 1. Undeploy old war"
result1=`curl -S -H "content-Type: application/json" -d '{"operation":"undeploy", "address":[{"deployment":"webapp.war"}]}' --digest $JBOSS_ADDR/management`
echo $result1
echo "-----"

echo "Step 2. Remove old war"
result2=`curl -S -H "content-Type: application/json" -d '{"operation":"remove", "address":[{"deployment":"webapp.war"}]}' --digest $JBOSS_ADDR/management`
echo $result2
echo "-----"

echo "Step 3. Upload new war"
bytes_value=`curl -F "file=@some-webapp/build/libs/webapp.war" --digest $JBOSS_ADDR/management/add-content | perl -pe 's/^.*"BYTES_VALUE"\s*:\s*"(.*)".*$/$1/'`
echo "Uploaded "$bytes_value
json_string_start='{"content":[{"hash":{"BYTES_VALUE" : "'
json_string_end='"}}], "address": [{"deployment":"webapp.war"}], "operation":"add", "enabled":"true"}'
json_string="$json_string_start$bytes_value$json_string_end"
echo "-----"

echo "Step 4. Deploy new war"
result=`curl -S -H "content-Type: application/json" -d "$json_string" --digest $JBOSS_ADDR/management | perl -pe 's/^.*"outcome"\s*:\s*"(.*)".*$/$1/'`
echo $result

if [ "$result" != "success" ]; then
   exit -1
fi
