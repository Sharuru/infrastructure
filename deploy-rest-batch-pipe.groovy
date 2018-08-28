#!groovy 

/**
 * 
 * This script is for Jenkins deployment of project XXXX
 *
 **/

// environment variable, if not passed from command line
// server username
env.DIST_USER= (env.DIST_USER != null) ? env.DIST_USER : "server-user"
// server ip
env.DIST_SERVER= (env.DIST_SERVER != null) ? env.DIST_SERVER : "127.0.0.1"
// source code location
env.GIT_REPO_URL=(env.GIT_REPO_URL != null) ? env.GIT_REPO_URL : "http://www.github.com/user/repo"
// soruce repo branch
env.GIT_BRANCH_NAME= (env.GIT_BRANCH_NAME != null) ? env.GIT_BRANCH_NAME : "*/master"
// current unused setting
env.SUB_SYSCD = "xx"
env.SUB_SYSCD_U = "XX"

// build target
def defaultManagementUnitStr=
'[{projectName:"project-name-1",subsystemCode:"xx", managementUnit:"xx1"}'+
',{projectName:"project-name-2",subsystemCode:"xx", managementUnit:"xx2"}'+
',{projectName:"project-name-3",subsystemCode:"xx", managementUnit:"xx3"}]'

// fallback
def managementUnitJSON = (env.managementUnitJSON != null) ? env.managementUnitJSON : defaultManagementUnitStr
def managementUnitList = readJSON text: managementUnitJSON

node('master') {

    stage('Prepartion'){
        echo "Fetching source code..."
        checkout changelog: true, poll: true, scm: [$class: 'GitSCM', 
        branches: [[name: "${GIT_BRANCH_NAME}"]], browser: [$class: 'GitLab', 
        repoUrl: "${GIT_REPO_URL}", version: '8.8'],
        doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: 'XXXXX-XXXXX-XXXXX-XXXXX',
        url: "${GIT_REPO_URL}.git"]]]
    }
    stage('Build'){
        echo "Now building..."

        managementUnitList.each{
            sh "/opt/gradle-3.3/bin/gradle ${it.projectName}:assemble"
        }
        
        echo "Build finished."
    }
    stage("Deploy"){
        echo "Now deploying..."

        def parallelSteps = managementUnitList.collectEntries {
            ["echoing ${it.projectName}" : deployBatchArtifact(it)]
        }
        parallel parallelSteps
    }
}

def deployBatchArtifact(mUnit) {
    return{
        timestamps{
            stage(mUnit.projectName){

                // if no setting, task abort
                assert mUnit.projectName!=null && mUnit.subsystemCode!=null && mUnit.managementUnit!=null
            
                echo "Starting deploy project: ${mUnit.projectName}"

                // if is base server
                if (mUnit.managementUnit.endsWith("00")){
                    sh """
                    echo "Base server deployment"
                    
                    # folder preparation
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p /xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01"
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p /xxxx/${mUnit.subsystemCode}/logs/${mUnit.managementUnit}_01"

                    # remove old libs
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "rm -rf /xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01/java"
                    
                    # transfer new libs
                    scp ${mUnit.projectName}/build/distributions/java.zip ${DIST_USER}@${DIST_SERVER}:/xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01/java.zip
                    
                    # unzip libs
                    ssh ${DIST_USER}@${DIST_SERVER} "cd /xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01; unzip -o -q java.zip"

                    # remove zipped libs
                    ssh ${DIST_USER}@${DIST_SERVER} "rm -f /xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01/java.zip"
                    
                    # transfer scripts
                    scp -r ${mUnit.projectName}/scripts/_01/bin ${DIST_USER}@${DIST_SERVER}:/xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01
                    scp -r ${mUnit.projectName}/scripts/_01/common ${DIST_USER}@${DIST_SERVER}:/xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01
                    
                    # add execute permission on scripts
                    ssh ${DIST_USER}@${DIST_SERVER} chmod +x -R /xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}_01/bin
                    """
                } else {
                    sh """
                    echo "Function deployment"

                    # folder preparation
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p /xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}"
                    ssh ${DIST_USER}@${DIST_SERVER} -o StrictHostKeyChecking=no "mkdir -p /xxxx/${mUnit.subsystemCode}/logs/${mUnit.managementUnit}"

                    # transfer scripts
                    scp -r ${mUnit.projectName}/scripts/bin ${DIST_USER}@${DIST_SERVER}:/xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}
                    scp -r ${mUnit.projectName}/scripts/common ${DIST_USER}@${DIST_SERVER}:/xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}

                    # add execute permission on scripts
                    ssh ${DIST_USER}@${DIST_SERVER} chmod +x -R /xxxx/${mUnit.subsystemCode}/batch/${mUnit.managementUnit}/bin
                    """
                }

                echo "Deployment of project: ${mUnit.projectName} is finished."
            }
        }
    }
}

