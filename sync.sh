#!/bin/bash
echo "=============================="
echo "Job STARTED at " $(date)
echo "Syncing project..."
cd /home/git/sync/lib && git pull origin && git push mirror
echo "Trigging Redmine API..."
curl "http://redmine.host/sys/fetch_changesets?key=token"
echo "Job FINISHED at " $(date)
echo "=============================="
