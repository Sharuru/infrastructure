#!groovy 

/**
 * バッチの自動リリースを実現するためのソースコードサンプルです。利用するには、
 * Jenkinsのitemを新規に作成し、アイテムの種類選択でパイプラインを選択します。
 * 設定の中にpipeline scriptを張り付ける箇所があるため、そこにこのスクリプトを張り付けてください。
 * v1.0.0
 * 
 **/

//環境変数の設定。Jenkinsから引数が渡されていればそちらを優先する。設定されていなければここで指定した値が利用される
//最初に会社コードと環境情報を取得しておく
deployTarget=readJSON text: env.deployTargetJson
//デプロイ先サーバIP　or サーバ名
env.DIST_SERVER= (deployTarget.serverName != null) ? deployTarget.serverName : "server.address"
//デプロイ先サーバログインID（公開鍵登録済であることが条件です）
env.DIST_USER= (deployTarget.userName != null) ? deployTarget.userName : "user"
//デプロイ先ルートディレクトリ(直接リリースとAnsibleでパスが異なるので注意)
env.DIST_PATH= (env.DIST_PATH != null) ? env.DIST_PATH : "/workarea"
//ソースコード取得元gitリポジトリURL
env.GIT_REPO_URL=(env.GIT_REPO_URL != null) ? env.GIT_REPO_URL : "http://git.repo.address/repo"
//ソースコード取得元gitブランチ名
env.GIT_BRANCH_NAME= (env.GIT_BRANCH_NAME != null) ? env.GIT_BRANCH_NAME : "*/some/branch"

//ビルド対象のサブシステム情報。引数が渡されなかった場合のみこの設定を利用します。
//Jenkinsのパラメータで同様の値を渡した場合は、そちらの値が優先されます。
//ProjectName ... サブプロジェクト単位名称
//subsystemCode ... サブシステムコード
//managementUnit ... 管理単位名称(リリース先dir)
def defaultManagementUnitStr=
'[{projectName:"xx00-batch-restserver",subsystemCode:"xx", managementUnit:"xx00_01"}'+
',{projectName:"xx99-common-batch",subsystemCode:"xx", managementUnit:"xx99"}]'
//リリースするscriptsの対象ディレクトリ
scriptDir=["anytran","bin","shell","param","sql"]

//----------------------------------設定ここまで（以下はソースコードです。修正は自己責任でお願いします。）-------------------------------------------------------//


//リリース対象のシステムが指定されてい無ければ、プログラムで指定されている値を利用する。
def managementUnitJSON = (env.managementUnitJSON != null && env.managementUnitJSON != '[一括デプロイ]') ? env.managementUnitJSON : defaultManagementUnitStr
def managementUnitList = readJSON text: managementUnitJSON

node('master') {

    try{
        stage('ソースコード取得'){
            echo "gitからコードを取得します"
            //ここのクレデンシャルIDは環境毎に設定必要かも。
            checkout changelog: true, poll: true, scm: [$class: 'GitSCM', 
            branches: [[name: "${GIT_BRANCH_NAME}"]], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'CleanBeforeCheckout'],
            [$class: 'CheckoutOption', timeout: 60],
            [$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 60]],
            browser: [$class: 'GitLab', 
            repoUrl: "${GIT_REPO_URL}", version: '8.8'],
            userRemoteConfigs: [[credentialsId: 'SOME_USER',
            url: "${GIT_REPO_URL}.git"]]]
        }
        stage('ビルド'){
            echo "ビルドします（一回だけ）"

            echo "プロキシ無効します"
            sh """
                cat gradle.properties
                sed -i 's/systemProp.http/#systemProp.http/g' gradle.properties
                cat gradle.properties
            """

            if( env.managementUnitJSON == '[一括デプロイ]') {
                // 一括デプロイの場合
                sh "chmod +x ./gradlew"
                sh "./gradlew clean assemble --refresh-dependencies -xtest"
            } else {
                // 個別ビルドの場合は先頭のプロジェクトのみビルド対象
                sh "chmod +x ./gradlew"
                sh "./gradlew clean ${managementUnitList.first().projectName}:assemble --refresh-dependencies -xtest"
            }

            echo "バッチを順番にデプロイします"
            managementUnitList.each {
                deployBatchArtifact(it)
            }
        }
    }catch(err){
        currentBuild.result = 'FAILURE'
        throw err
    }finally{
        stage('クリンナップ'){
            echo '実行完了'
        }
    }
}

/**
 * 引数に従って一つの管理単位のデプロイ処理を行います。
 * mUnit 処理単位の管理単位情報
 **/
def deployBatchArtifact(mUnit) {
        timestamps{
            stage(mUnit.projectName +'(' + mUnit.managementUnit + ')'){
                
                echo env.DIST_SERVER
                echo "${deployTarget}"
                echo "${deployTarget.corpCd}"

                //引数をチェックし、値が設定されていなければ強制終了
                assert mUnit.projectName!=null && mUnit.subsystemCode!=null && mUnit.managementUnit!=null && deployTarget.corpCd!=null

                if(mUnit.projectName == 'xx00-batch-restserver') {
                    echo "プロジェクト名 ${mUnit.projectName}(${mUnit.managementUnit})のデプロイを開始します"
                    // script配置用に管理単位の末尾を取得
                    def managementUnitSuffix = mUnit.managementUnit.substring(4)
                    
                    // 管理単位配下を全削除して作業する
                    sh """
                    # 管理単位フォルダを削除（クリーン）存在しないときにはエラーにならないようにする。
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "rm -rf ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/"

                    # 管理単位フォルダを作成(Ansibleの場合、logsフォルダの作成は不要)
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}"
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/logs/${mUnit.managementUnit}"
                    """
                    
                    sh """
                    # javaファイルを転送
                    scp ${mUnit.projectName}/build/distributions/java.zip ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/java.zip

                    # ファイルを解凍して展開（一旦ディレクトリに移動してからunzip)
                    ssh ${DIST_USER}@${DIST_SERVER} "cd ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}; 7z x -aoa java.zip"

                    # 不要なzipファイルを削除
                    ssh ${DIST_USER}@${DIST_SERVER} "rm -f ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/java.zip"

                    # jco設定ファイルの転送
                    scp ${mUnit.projectName}/libs/libsapjco3.so ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/java/lib/libsapjco3.so

                    # スクリプトファイルを転送(scriptsフォルダ配下丸ごと）←ワンソースではこれ禁止
                    #scp -r ${mUnit.projectName}/scripts/${managementUnitSuffix}/* ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}

                    # スクリプトファイルを転送(フォルダごとに削除&再作成)
                    # commonフォルダは会社コードないのでまとめて転送
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "rm -rf ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/common/"

                    # I-SOLVER カスタム
                    wget http://external.address.config/some.conf
                    mv some.conf ${mUnit.projectName}/scripts/${managementUnitSuffix}/common/batch.conf

                    scp -r ${mUnit.projectName}/scripts/${managementUnitSuffix}/common ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/
                    """

                    //binフォルダは指定会社のみ削除して転送
                    //binフォルダがgitに存在する場合のみ実施
                    if(fileExists("${mUnit.projectName}/scripts/${managementUnitSuffix}/bin/${deployTarget.corpCd}") || fileExists("${mUnit.projectName}/scripts/${managementUnitSuffix}/bin/X999")){
                        sh """
                        ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "rm -rf ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/bin/${deployTarget.corpCd}/"
                        ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/bin/${deployTarget.corpCd}"
                        scp -r ${mUnit.projectName}/scripts/${managementUnitSuffix}/bin/X999/* ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/bin/${deployTarget.corpCd}/ 2>/dev/null || :
                        scp -r ${mUnit.projectName}/scripts/${managementUnitSuffix}/bin/${deployTarget.corpCd} ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/bin/ 2>/dev/null || :
                        """
                    }

                    // paramフォルダがgitに存在する場合のみ実施
                    if(fileExists("${mUnit.projectName}/scripts/${managementUnitSuffix}/param/${deployTarget.corpCd}") || fileExists("${mUnit.projectName}/scripts/${managementUnitSuffix}/param/X999")){
                        sh """
                        ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "rm -rf ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/param/${deployTarget.corpCd}/"
                        ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/param/${deployTarget.corpCd}"
                        scp -r ${mUnit.projectName}/scripts/${managementUnitSuffix}/param/X999/* ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/param/${deployTarget.corpCd}/ 2>/dev/null || :
                        scp -r ${mUnit.projectName}/scripts/${managementUnitSuffix}/param/${deployTarget.corpCd} ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/param/ 2>/dev/null || :
                        """
                    }

                    sh """
                    # スクリプトファイルに実行権限を付与(Ansibleの場合、権限付与は不要)
                    ssh ${DIST_USER}@${DIST_SERVER} chmod +x -R ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/bin/${deployTarget.corpCd}
                    """

                } else {

                    echo "プロジェクト名${mUnit.projectName}のデプロイを開始します"

                    sh """
                    # 管理単位フォルダを削除（クリーン）(管理単位フォルダごと削除はしない)
                    #ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "rm -rf ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/"

                    # 管理単位フォルダを作成(Ansibleの場合、logs,dataフォルダの作成は不要)
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}"
                    #ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/logs/${mUnit.managementUnit}"
                    #ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/data/${mUnit.managementUnit}"
                    """
                    //scripts配下のフォルダ転送
                    echo "${deployTarget.targetName}へ${deployTarget.corpCd}のscriptファイルリリースを実施します。"
                    sh """
                    # commonフォルダを転送(ここは会社コードの区別がない前提)
                    scp -r ${mUnit.projectName}/scripts/common ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/
                    """
                    //会社コードごとにフォルダが分かれているscriptを転送
                    scriptDir.each {
                        //対象のフォルダがgitに存在する場合のみ実施
                        echo "${mUnit.projectName}/scripts/${it}/${deployTarget.corpCd}"
                        if(fileExists("${mUnit.projectName}/scripts/${it}/${deployTarget.corpCd}") || fileExists("${mUnit.projectName}/scripts/${it}/X999")){
                            sh """
                            #対象ディレクトリの削除
                            ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "rm -rf ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/${it}/${deployTarget.corpCd}"

                            #対象ディレクトリを作成
                            ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/${it}/${deployTarget.corpCd}"

                            #リリース対象のファイルを転送(X999の後に個社ファイル)
                            scp -r ${mUnit.projectName}/scripts/${it}/X999/* ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/${it}/${deployTarget.corpCd}/ 2>/dev/null || :
                            scp -r ${mUnit.projectName}/scripts/${it}/${deployTarget.corpCd} ${DIST_USER}@${DIST_SERVER}:${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/${it} 2>/dev/null || :
                            """

                            if("${it}"=="bin"){
                                sh """
                                # スクリプトファイルに実行権限を付与
                                ssh ${DIST_USER}@${DIST_SERVER} chmod +x -R ${DIST_PATH}/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/${it}
                                """
                            }
                        }
                    }
                    echo "${deployTarget.targetName}へ${deployTarget.corpCd}のscriptファイルのリリースが完了"
                }
            }
        }
}
