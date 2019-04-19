#!/bin/bash
_REDMINE_PSQL_CONTAINER="75625d30e75c"
_REDMINE_APP_CONTAINER="b9600fdb2ecb"
_CONFLUENCE_PSQL_CONTAINER="a7f08e941820"

echo "====="
echo "Job STARTED at " $(date)
echo "-----"
echo "Step1. Redmine native backups"
echo "-----"
echo "Preparing..."
rm -rf /var/lib/docker/volumes/redmine-pack/_data/backups/*
echo "Backing up..."
docker exec -it ${_REDMINE_PSQL_CONTAINER} redmine-backup-create
echo "Transferring..."
rsync -av /var/lib/docker/volumes/redmine-pack/_data/backups/*.tar backup@another.host:/home/backup/backups/redmine
echo "Cleaning up..."
rm -rf /var/lib/docker/volumes/redmine-pack/_data/backups/*
echo "-----"
echo "Step2. Redmine general backups"
echo "-----"
echo "Dumping databases..."
docker exec -it ${_REDMINE_APP_CONTAINER} sh -c "pg_dump -v -Fc redmine -U redmine > /opt/container-shares/redmine.backup"
echo "Transferring database dumps..."
rsync -av /var/lib/docker/volumes/container-shares/_data/redmine.backup backup@another.host:/home/backup/backups/redmine
echo "Transferring resources..."
ssh backup@another.host "mkdir -p /home/backup/backups/redmine/resources/data"
ssh backup@another.host "mkdir -p /home/backup/backups/redmine/resources/pack"
ssh backup@another.host "mkdir -p /home/backup/backups/redmine/resources/plugin"
rsync -av /var/lib/docker/volumes/redmine-files/_data backup@another.host:/home/backup/backups/redmine/resources/data
rsync -av /var/lib/docker/volumes/redmine-pack/_data backup@another.host:/home/backup/backups/redmine/resources/pack
rsync -av /var/lib/docker/volumes/redmine-plugin/_data backup@another.host:/home/backup/backups/redmine/resources/plugin
echo "Packing resouces..."
ssh backup@another.host "rm /home/backup/backups/redmine/redmine-resources.tar.gz"
ssh backup@another.host "tar -I pigz -pcvf /home/backup/backups/redmine/redmine-resources.tar.gz -C /home/backup/backups/redmine resources"
echo "Cleaning up working files..."
rm /var/lib/docker/volumes/container-shares/_data/redmine.backup
#ssh backup@another.host "rm -rf /home/backup/backups/redmine/resources"
echo "-----"
echo "Step3. Confluence general backups"
echo "Skipped."
echo "-----"
echo "Step4. Confluence native backups"
echo "-----"
echo "Dumping databases..."
docker exec -it ${_CONFLUENCE_PSQL_CONTAINER} sh -c "pg_dump -v -Fc confluence -U postgres > /opt/container-shares/confluence.backup"
echo "Transferring database dumps..."
rsync -av /var/lib/docker/volumes/container-shares/_data/confluence.backup backup@another.host:/home/backup/backups/confluence
echo "Transferring resources..."
ssh backup@another.host "mkdir -p /home/backup/backups/confluence/resources/application-data"
ssh root@backup.host "rsync -av /var/atlassian/application-data backup@another.host:/home/backup/backups/confluence/resources/application-data"
echo "Packing resouces..."
ssh backup@another.host "rm /home/backup/backups/confluence/confluence-resources.tar.gz"
ssh backup@another.host "tar -I pigz -pcvf /home/backup/backups/confluence/confluence-resources.tar.gz -C /home/backup/backups/confluence resources"
echo "Cleaning up working files..."
rm /var/lib/docker/volumes/container-shares/_data/confluence.backup
#ssh backup@another.host "rm -rf /home/backup/backups/confluence/resources"
echo "====="
echo "Job FINISHED at " $(date)
