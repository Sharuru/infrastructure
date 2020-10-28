#!groovy

/**
 * 自動テストをデバッグするためのパイプラインスクリプトです。
 **/

// 環境変数の設定。Jenkinsから引数が渡されていればそちらを優先する。設定されていなければここで指定した値が利用される
// Dockerイメージ
env.CONF_DOCKER_IMAGE_NAME = 'katalonstudio/katalon'
// Dockerイメージのバージョン
env.CONF_DOCKER_IMAGE_VERSION = (env.CONF_DOCKER_IMAGE_VERSION != null) ? env.CONF_DOCKER_IMAGE_VERSION : '7.4.0'
// Dockerボリュームマウントフォルダ
env.CONF_DOCKER_VOLUME_ROOT = '/workspace'
// 実行ブラウザ
env.CONF_KRE_BROWSER_TYPE = 'Chrome'
// 実行画面サイズ
env.CONF_KRE_DISPLAY_CONFIG = '1920x1080x24'
// デフォルトコマンドパラメータ
env.CONF_KRE_CONFIG_PARAMS = '-proxy.option=MANUAL_CONFIG -proxy.server.type=HTTP -proxy.server.address=127.0.0.1 -proxy.server.port=12080 -proxy.excludes="localhost,127.0.0.1"'
// 成果物退避サーバー
env.CONF_STORAGE_SERVER = 'user@server'
// 成果物退避フォルダ
env.CONF_STORAGE_ROOT = '/workarea/debug'
// Katalon Runtime Engine APIキー
// env.CONF_KRE_API_KEY = env.CONF_KRE_API_KEY

// テストソースブランチ
env.TASK_TEST_BRANCH = (env.TASK_TEST_BRANCH != null) ? env.TASK_TEST_BRANCH : '*/develop'
// 実行ケースフォルダ
env.TASK_TEST_CASE_ROOT = (env.TASK_TEST_CASE_ROOT != null) ? env.TASK_TEST_CASE_ROOT : 'Test Suites/SuitesSource/'
// 実行ケースタイプ
env.TASK_TEST_CASE_TYPE = env.TASK_TEST_CASE_TYPE
// 実行プロフィール
env.TASK_TEST_EXECUTE_PROFILE = (env.TASK_TEST_EXECUTE_PROFILE != null) ? env.TASK_TEST_EXECUTE_PROFILE : 'Default'
// 実行ケース
env.TASK_TEST_CASE = env.TASK_TEST_CASE

//----------------------------------設定ここまで（以下はソースコードです。修正は自己責任でお願いします。）-------------------------------------------------------//

def testPlan = env.TASK_TEST_PLAN
def CLICommandHead = 'docker run -t --rm ' + 
'-v $(pwd):' + env.CONF_DOCKER_VOLUME_ROOT + ' ' + 
'--network host ' + 
'--env KATALON_USER_ID=`id -u $USER` ' + 
'--env DISPLAY_CONFIGURATION=' + env.CONF_KRE_DISPLAY_CONFIG + ' ' +
'--env LANG=C.UTF-8 ' + 
env.CONF_DOCKER_IMAGE_NAME + ':' + env.CONF_DOCKER_IMAGE_VERSION + ' ' +
'katalonc.sh ' + 
'-projectPath=' + env.CONF_DOCKER_VOLUME_ROOT + ' ' +
'-browserType=' + env.CONF_KRE_BROWSER_TYPE + ' ' +
'-retry=0 ' + 
'-statusDelay=15 ' + 
'-executionProfile=' + env.TASK_TEST_EXECUTE_PROFILE + ' ' + 
'-apiKey=$CONF_KRE_API_KEY '
def CLICommandTail = '--config ' + env.CONF_KRE_CONFIG_PARAMS
def CLIProjectParams = ''

node('master') {
    try {
        stage('実行準備') {
            echo 'ワークスペース内古いファイルを削除します'
            sh """
                rm -rf Reports/*
                rm -rf Materials/*
                rm -rf Storage/*
            """
            echo 'Gitからソースコードを取得します'
            checkout changelog: true, poll: true, scm: [$class: 'GitSCM',
            branches: [[name: "${TASK_TEST_BRANCH}"]], browser: [$class: 'GitLab',
            repoUrl: "http://server.address:port/repo", version: '8.8'],
            doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: 'XXXX-XXXX-XXXX-XXXX',
            url: "http://server.address:port/repo.git"]]]
            echo '外部ストレージからエビデンスを取得します'
            sh """
                rsync -azvP ${CONF_STORAGE_SERVER}:${CONF_STORAGE_ROOT}/Storage/ Storage/
            """
        }
        stage('テスト実行') {
            echo '実行コマンドを構成します'
            def caseIds = env.TASK_TEST_CASE.split(',')
            for(String caseId : caseIds ) {
                if (env.TASK_TEST_CASE_TYPE == 'COLLECTION'){
                    CLIProjectParams = '-testSuiteCollectionPath="' + env.TASK_TEST_CASE_ROOT + caseId.replaceAll("\\s","") + '" '
                } else {
                    CLIProjectParams = '-testSuitePath="' + env.TASK_TEST_CASE_ROOT + caseId.replaceAll("\\s","") + '" '
                }
                def CLICommand = CLICommandHead + ' ' + CLIProjectParams + ' ' + CLICommandTail
                withCredentials([string(credentialsId: 'CONF_KRE_API_KEY', variable: 'CONF_KRE_API_KEY')]) {
                    sh """
                      echo 'コマンド【${CLICommand}】を実行する'
                      ${CLICommand}
                    """
                }
            }
        }
    } catch (ex) {
        currentBuild.result = 'FAILURE'
        throw ex
    } finally {
        if(env.CONF_ALWAYS_UPLOAD_EVIDENCE.toBoolean()){
            stage('実行後処理') {
                echo '外部ストレージへエビデンスを退避します'
                sh """
                    rsync -azvP Storage/ ${CONF_STORAGE_SERVER}:${CONF_STORAGE_ROOT}/Storage/
                """
                echo 'エビデンス退避完了'
                echo '----------'
                echo '外部ストレージへテストレポートを退避します'
                def archiveDate = new Date().format("yyyyMMddHHmmss")
                echo 'テストレポート退避名：' + archiveDate + '.tar.gz'
                sh """
                    tar -cvzf ${archiveDate}.tar.gz Result/
                    rsync --remove-source-files -azvP ${archiveDate}.tar.gz ${CONF_STORAGE_SERVER}:${CONF_STORAGE_ROOT}/Result/
                """
            }
        }
        stage('クリンナップ') {
            echo '実行完了'
        }
    }
}
