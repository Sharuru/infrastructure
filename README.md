# infrastructure

Implementing infrastructure as code.

- Count.java: count files under each folders.
- backup-prod.sh backup some webapp files to another server through many ways.
- backup.sh: used to backup docker volumes in the past.
- deploy-batch.sh: GitLab ci scripts to deploy Spring Batch to server.
- deploy-war: GitLab ci scripts to deploy war files to JBoss server.
- fake-backup.sh: fake backup job, called by cron to bypass the useless PMO security check :P
- parameter-mixin.sh: mixin other file with fixed line period.
- sync.sh: called by cron to sync between upstream git repostiory and local repository.


pipeline

- deploy-batch-local.groovy: deploy Spring Batch in another way.
- deploy-rest-batch-pipe.groovy: Jenkins script, used to deploy Spring Batch to specific servers in the past.
- run-all-jobs.groovy: call Jenkins job in sequence order.
- run-katalon-debug.groovy: run specific Katalon test case with docker image.
- run-specific-batch.groovy: execute specific batch(any command) on server.