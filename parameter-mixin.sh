#!/bin/bash
#===================================================================
# 処理概要: バッチパラメータミックスイン実行
# 備考: 実行時の引数
#       $1：バッチパラメータファイル
#       $2：ミックスインテンプレートファイル
#       $3：ミックスイン間隔（行単位）
# 作成日: 2020/11/01
# 会社名 作成者: 
#===================================================================

# 定数設定
_mixinFileSuffix="_mixin.param"
_mixinLineNoPrefix="CSVEXP"

# 
echo "*************************************************"
echo "バッチパラメータミックスイン"
echo "*************************************************"
echo "ミクスイン対象：$1"
echo "利用ンテンプレート：$2"
echo "ミックスイン間隔：毎 $3 行"
echo "*************************************************"

# ミクスイン対象バックアップ
#backupFileCount=$(ls -1q $1.bk.* 2> /dev/null | wc -l)
#backupFileCount=$((${backupFileCount}+1))
#backupFilePath=$1.bk.${backupFileCount}
#cp -p $1 ${backupFilePath}
#echo "ミクスイン対象バックアップ：${backupFilePath}"

# 新パラメータ作成
mixinFilePath=$(dirname $1)/$(basename $1 .param)${_mixinFileSuffix}
rm -f ${mixinFilePath}
touch ${mixinFilePath}
echo "新パラメータ作成：${mixinFilePath}"

echo "*************************************************"
echo "パラメータミックスイン開始"

# パラメータミクスイン
outputCount=0
mixinCount=1
while IFS="" read -r line || [ -n "$line" ]
do
    echo "$line" >> ${mixinFilePath}
    outputCount=$(($outputCount+1))
    # テンプレートミクスイン
    if [ $outputCount -eq $3 ]; then
        while IFS="" read -r templateLine || [ -n "$templateLine" ]
        do
            # 一覧行番号採番
            mixinLineNo=$(printf "%05d" ${mixinCount})
            mixinLineNo=${_mixinLineNoPrefix}${mixinLineNo}
            echo "${mixinLineNo} $templateLine" >> ${mixinFilePath}
            mixinCount=$(($mixinCount+1))
        done < $2
        outputCount=0
    fi
done < $1

# パラメータ最後行処理
while IFS="" read -r templateLine || [ -n "$templateLine" ]
do
    mixinLineNo=$(printf "%05d" ${mixinCount})
    mixinLineNo=${_mixinLineNoPrefix}${mixinLineNo}
    echo "${mixinLineNo} $templateLine" >> ${mixinFilePath}
    mixinCount=$(($mixinCount+1))
done < $2
# 最後の改行文字を削除
#truncate -s -1 ${mixinFilePath}

echo "*************************************************"
echo "パラメータミックスイン完了"
echo "*************************************************"
