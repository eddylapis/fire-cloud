pipeline {
    agent any

    triggers {
        pollSCM('H/1 0 * * *')
    }
    tools {
        maven 'maven-3.10.0'
        jdk 'JDK8'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    environment {
        _GITHUB_READABLE_CREDENTIALS = 'dcae8179-aec2-4eb5-b6ce-177179d463c5'
        _need_deploy_to_nexus = "${params.needDeploy}"
    }

    parameters {
        string(defaultValue: "fire-gateway/pom.xml", name:'pomPath', description: 'pom文件相对路径')
        string(defaultValue: 'https://github.com/beifei1/fire-cloud.git', name: 'repoUrl', description: '代码仓库路径')
        string(defaultValue: 'master', name: 'repoBranch', description: '拉取的代码分支')
        string(defaultValue: 'fire-gateway',name: 'appName', description: '应用名称')
        string(defaultValue: 'cn.fire', name: 'groupId', description: 'Maven组')
        string(defaultValue: '0.0.1-SNAPSHOT', name: 'version', description: '制品版本')
        choice(name:'needDeploy',choices:'False\nTrue',description:'是否发布到私服')
    }

    stages {
        stage('代码获取') {
            steps {
                echo "staring fetch code from ${params.repoUrl}..."
                git credentialsId: "${_GITHUB_READABLE_CREDENTIALS}", url: "${params.repoUrl}", branch: "${params.repoBranch}"
                echo "fetch code complete !"
            }
        }

        stage ("构建发布") {
            steps {
               configFileProvider([configFile(fileId: 'd4231502-faae-45f4-b0d9-c4bff6e15692',targetLocation: 'setting.xml', variable: 'MAVEN_GLOBALE_SETTING')]) {
                   sh "pwd"
                   script {
                        println "${_need_deploy_to_nexus}"
                   }
                   sh "mvn -f ${params.pomPath} -s $MAVEN_GLOBALE_SETTING deploy -Dmaven.skip.test=true"
               }
//               nexusPublisher (
//                    nexusInstanceId: 'nexus3',
//                    nexusRepositoryId: 'maven-snapshots',
//                    packages: [[
//                        $class: 'MavenPackage',
//                        mavenAssetList: [[classifier: '',extension: '',filePath: "${params.appName}/target/${params.appName}-${params.version}.jar"]],
//                        mavenCoordinate: [artifactId: "${params.appName}", groupId: "${params.groupId}",packaging: 'jar', version: "${params.version}"]
//                    ]]
//                )
            }
        }

        stage('应用部署') {
            steps {
                echo 'starting deploy'
            }
        }
    }
    post {
        always {
            echo 'staring clean workspace'
//            cleanWs()
        }
    }
}