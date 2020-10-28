#!groovy 

//環境変数の設定。Jenkinsから引数が渡されていればそちらを優先する。設定されていなければここで指定した値が利用される

// サーバー情報リスト
executeServer = readJSON text: env.executeServerJson
// サーバー情報
env.EXECUTE_SERVER = executeServer.serverName
// ジョブ実行ユーザー情報
env.EXECUTE_USER = executeServer.userName
// 事前設定されたバッチリスト
executeBatchJob = readJSON text: env.executeBatchJobJson

// ユーザー入力バッチ
executeBinPath = env.executeBinPath
executeParamPath = env.executeParamPath
executeBatchName = 'カスタム'


//----------------------------------設定ここまで（以下はソースコードです。修正は自己責任でお願いします。）-------------------------------------------------------//

node('master') {

    try{
        stage('バッチジョブ実行'){
            if(executeBinPath == '' || executeParamPath == ''){
                echo '事前設定されたバッチを実行する'
                executeBinPath = executeBatchJob.binPath
                executeParamPath = executeBatchJob.paramPath
                executeBatchName = executeBatchJob.jobName
            } else {
                echo 'ユーザー入力バッチを実行する'
            }
            echo '実行サーバー：' + executeServer.serverName
            echo '実行ジョブ：' + executeBatchName
            echo '実行パス：' + executeBinPath
            echo '実行パラメータ：' + executeParamPath

            echo 'REST バッチの実行リクエストを送信する'
            sh """
                ssh ${EXECUTE_USER}@${EXECUTE_SERVER} -o StrictHostKeyChecking=no "${executeBinPath} ${executeParamPath}"
            """
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
