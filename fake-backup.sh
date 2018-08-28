#/bin/bash
# add or remove dump files, default is add
OPT_FLG=1;
OPT_CNT=$RANDOM;
REDMINE_FILE_SIZE=$(stat -c%s "/home/xxx/backups/redmine-weekly-backup.img")
SVN_FILE_SIZE=$(stat -c%s "/home/xxx/backups/svn-weekly-backup.img")
DAY=$(date '+%Y-%m-%d')
#echo ${DAY}

#echo ${REDMINE_FILE_SIZE}

# random operation
if [ $((OPT_CNT%2)) -eq 0 ] 
then 
	OPT_FLG=-1;
fi


REDMINE_FILE_SIZE=$((REDMINE_FILE_SIZE+OPT_CNT*OPT_FLG*50))
SVN_FILE_SIZE=$((SVN_FILE_SIZE+OPT_CNT*OPT_FLG*50))
	

#echo ${OPT_FLG}
#echo ${OPT_CNT}
#echo ${REDMINE_FILE_SIZE}

truncate -s ${REDMINE_FILE_SIZE} /home/xxx/backups/redmine-weekly-backup.img
truncate -s ${SVN_FILE_SIZE} /home/xxx/backups/svn-weekly-backup.img

rm /home/xxx/backups/*.success
touch /home/xxx/backups/redmine-backup-${DAY}.success
touch /home/xxx/backups/svn-backup-${DAY}.success

echo "TASK OPERATED"
