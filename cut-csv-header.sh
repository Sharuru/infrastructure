#!/bin/bash
FILE=$1

echo "====="
echo "Reading..."
echo $FILE
echo "====="

o_header=$( head -n 1 $FILE )
IFS=',' read -ra o_header_array <<< $o_header;
echo "Original header is:"
idx=1
for i in ${o_header_array[@]}
do
    echo ${idx}_$i 
    let idx=${idx}+1
done

echo "====="
trimmed_pos=""
idx=1
for header_name in ${o_header_array[@]}
do
  if [ "$header_name" == "XX_HEADER_NAME_1" ] || [ "$header_name" == "XX_HEADER_NAME_2" ]
  then
    echo "$header_name detected at index $idx"
    trimmed_pos="$trimmed_pos,$idx"
  fi
  idx=$((idx+1))
done
trimmed_pos="${trimmed_pos:1}"
echo "====="
#echo "cp $1 $1.bk"
#cp $1 $1.bk
echo "cut -d, -f$trimmed_pos --complement $1 > $2"
cut -d, -f$trimmed_pos --complement $1 > $2