#!groovy

// 実行プロフィール
env.TASK_TEST_EXECUTE_PROFILE = (env.TASK_TEST_EXECUTE_PROFILE != null) ? env.TASK_TEST_EXECUTE_PROFILE : 'Configuration3'

//----------------------------------設定ここまで（以下はソースコードです。修正は自己責任でお願いします。）-------------------------------------------------------//

node('master') {
    try {
        stage('順番実行') {
            echo '本コレクション内のジョブを順番で実行する'
        }
        stage ('コレクション【０１】実行') {
            build job: 'collection-01', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【０２】実行') {
            build job: 'collection-02'
        }
        stage ('コレクション【０３】実行') {
            build job: 'collection-03', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【０４】実行') {
            build job: 'collection-04'
        }
        stage ('コレクション【０５】実行') {
            build job: 'collection-05', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【０６】実行') {
            build job: 'collection-06'
        }
        stage ('コレクション【０７】実行') {
            build job: 'collection-07', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【０８】実行') {
            build job: 'collection-08'
        }
        stage ('コレクション【０９】実行') {
            build job: 'collection-09', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【１０】実行') {
            build job: 'collection-10'
        }
        stage ('コレクション【１１】実行') {
            build job: 'collection-11', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【１２】実行') {
            build job: 'collection-12'
        }
        stage ('コレクション【１３】実行') {
            build job: 'collection-13', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【１４】実行') {
            build job: 'collection-14'
        }
        stage ('コレクション【１５】実行') {
            build job: 'collection-15', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【１６】実行') {
            build job: 'collection-16'
        }
        stage ('コレクション【１７】実行') {
            build job: 'collection-17', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【１８】実行') {
            build job: 'collection-18'
        }
        stage ('コレクション【１９】実行') {
            build job: 'collection-19', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【２０】実行') {
            build job: 'collection-20'
        }
        stage ('コレクション【２１】実行') {
            build job: 'collection-21', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【２２】実行') {
            build job: 'collection-22'
        }
        stage ('コレクション【２３】実行') {
            build job: 'collection-23', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【２４】実行') {
            build job: 'collection-24'
        }
        stage ('コレクション【２５】実行') {
            build job: 'collection-25', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【２６】実行') {
            build job: 'collection-26'
        }
        stage ('コレクション【２７】実行') {
            build job: 'collection-27', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【２８】実行') {
            build job: 'collection-28'
        }
        stage ('コレクション【２９】実行') {
            build job: 'collection-29', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【３０】実行') {
            build job: 'collection-30'
        }
        stage ('コレクション【３１】実行') {
            build job: 'collection-31', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }
        stage ('コレクション【３２】実行') {
            build job: 'collection-32'
        }
        stage ('コレクション【３３】実行') {
            build job: 'collection-33', parameters: [[$class: 'ListSubversionTagsParameterValue', name: 'TASK_TEST_EXECUTE_PROFILE', tag: env.TASK_TEST_EXECUTE_PROFILE]]
        }

    } catch (ex) {
        currentBuild.result = 'FAILURE'
        throw ex
    } finally {
        stage('クリンナップ') {
            echo '実行完了'
        }
    }
}

