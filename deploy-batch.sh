#! /bin/bash
#####
# This file is used to deploy batch through ssh and scp
#####

# Configuration
SERVER_NAME="Server1"
SERVER_ADDR="user@hostname.com"
BATCH_ID="xx01"
BATCH_BUILD="xx01-some-batch"

echo "Deploy "${BATCH_BUILD^^}" to the server: "${SERVER_NAME}

echo "-----------------------------------"
echo "Step1. Configure SSH connection"
which ssh-agent || ( apt-get update -y && apt-get install openssh-client -y )
eval $(ssh-agent -s)
ssh-add <(echo "$SSH_KEY")
mkdir -p ~/.ssh
echo -e "Host *\n\tStrictHostKeyChecking no\n\n" > ~/.ssh/config

echo "-----------------------------------"
echo "Step2. Create remote directories"
ssh ${SERVER_ADDR} "rm -rf /project/xx/batch/${BATCH_ID}/"
ssh ${SERVER_ADDR} "mkdir -p /project/xx/batch/${BATCH_ID}"
ssh ${SERVER_ADDR} "mkdir -p /project/xx/logs/${BATCH_ID}"

echo "-----------------------------------"
echo "Step3. Push batch files"
scp -r ${BATCH_BUILD}/build/distributions/java.tar ${SERVER_ADDR}:/project/xx/batch/${BATCH_ID}
scp -r ${BATCH_BUILD}/scripts/* ${SERVER_ADDR}:/project/xx/batch/${BATCH_ID}

echo "----------------------------------"
echo "Step4. Perpare remote batch environments"
ssh ${SERVER_ADDR} "tar xvf /project/xx/batch/${BATCH_ID}/java.tar -C /project/xx/batch/${BATCH_ID}"
ssh ${SERVER_ADDR} "chmod +x /project/xx/batch/${BATCH_ID}/bin/files/*.sh"

echo "----------------------------------"
echo "Step5. Modify custom configuration"

ssh ${SERVER_ADDR} "touch /project/xx/batch/${BATCH_ID}/application.json"

ssh ${SERVER_ADDR} "echo '{' >> /project/xx/batch/${BATCH_ID}/application.json"
ssh ${SERVER_ADDR} "echo '  "\"spring.datasource.url"\": "\"jdbc:postgresql://database:5432/DB?currentSchema=projectdb"\",' >> /project/xx/batch/${BATCH_ID}/application.json"
ssh ${SERVER_ADDR} "echo '  "\"spring.datasource.username"\": "\"projectdb"\",' >> /project/xx/batch/${BATCH_ID}/application.json"
ssh ${SERVER_ADDR} "echo '  "\"spring.datasource.password"\": "\"password"\"' >> /project/xx/batch/${BATCH_ID}/application.json"
ssh ${SERVER_ADDR} "echo '}' >> /project/xx/batch/${BATCH_ID}/application.json"

ssh ${SERVER_ADDR} "echo '# Modified DB configuration' >> /project/xx/batch/${BATCH_ID}/common/batch.conf"
ssh ${SERVER_ADDR} "echo 'APPLICATION_JSON_FILE=/project/xx/batch/${BATCH_ID}/application.json' >> /project/xx/batch/${BATCH_ID}/common/batch.conf"

ssh ${SERVER_ADDR} "rm /project/xx/batch/${BATCH_ID}/java.tar"

echo "----------------------------------"
echo "task finished."
