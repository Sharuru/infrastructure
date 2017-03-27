#!/bin/bash
####################################
#
# Backup script
#
# This script will backup files in path and 
# transfered to destination as .tar
#
####################################

# get date
date=$(date +%Y-%m-%d)

# backup origin
backup_files=(
"/path/to/file"
)

# for tar file
filenames=(
"backup-filename"
)

# backup destination
dest="/path/to/location"

# setup variables for the archive filename.
day=$(date +%Y-%m-%d-%s)

for i in "${!backup_files[@]}"
do

filename="${filenames[$i]}"

# create archive filename.
archive_file="$filename-$day.tar.gz"

# print start status message.
echo "Backing up ${backup_files[$i]} to $dest/$date/$archive_file"
date
echo

# create dir
mkdir -p $dest/$date

# backup the files using tar.
sudo tar pcf - ${backup_files[$i]} | pigz -9 -p 4 > $dest/$date/$archive_file

done

# print end status message.
echo
echo "Backup finished"
date

# long listing of files in $dest to check file sizes.
ls -lh $dest/$date

# scp to remote
scp -r $dest/$date user@remote.host:/path/to/remote/location
