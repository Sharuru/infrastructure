# infrastructure

Implementing infrastructure as code.

- backup.sh: used to backup docker volumes in the past.
- deploy-rest-batch-pipe.groovy: jenkins script, used to deploy Spring Batch to specific servers in the past.
- fake-backup.sh: fake backup job, called by cron to bypass the useless PMO security check :P
- deploy-war: gitlab ci scripts to deploy war files to JBoss server.
- deploy-batch: gitlab ci scripts to deploy Spring Batch to server.
- sync.sh: called by cron to sync between upstream git repostiory and local repository.
- backup-prod.sh backup some webapp files to another server through many ways.
- Count.java: count files under each folders.
