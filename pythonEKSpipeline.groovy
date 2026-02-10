def call(Map configMap) {
    pipeline {
    agent {
        node {
            label 'agent-1'
        }
    }
    environment {
        appVersion = ''
        AWS_REGION = 'us-east-1'
        PROJECT = configMap.get('project')
        COMPONENT = configMap.get('component')
        AWS_ACCOUNT_ID = '997416683845'

    }
    stages {
        stage('Read Version') {
            steps {
                script{
                    appVersion = readFile(file: 'version')
                    echo "app version: ${appVersion}"
                }
            }
        }
        stage ('Install Dependencies') {
            steps {
                script {
                    sh """
                        python3 -m venv venv
                        . venv/bin/activate
                        pip install -r requirements.txt
                    """
                }
                    
            }
        }
        stage ('Run Tests') {
            steps {
                sh 'echo "Running tests for version ${appVersion}"'
            }
        }

        // stage ('Depandabot scan'){
        //     environment {
        //         GITHUB_API = 'https://api.github.com'
        //         GITHUB_OWNER = 'Kishorep-08'
        //         GITHUB_REPO = 'catalogue'
        //         GITHUB_TOKEN = credentials('github-token')
        //     }
        //     steps {
        //         script {
        //             // sh '''
        //             // echo "Fetching Dependabot alerts..."

        //             // response=$(curl -s \
        //             //     -H "Authorization: token ${GITHUB_TOKEN}" \
        //             //     -H "Accept: application/vnd.github+json" \
        //             //     "${GITHUB_API}/repos/${GITHUB_OWNER}/${GITHUB_REPO}/dependabot/alerts?per_page=100")

        //             // echo "${response}" > dependabot_alerts.json

        //             // high_critical_open_count=$(echo "${response}" | jq '[.[] 
        //             //     | select(
        //             //         .state == "open"
        //             //         and (.security_advisory.severity == "high"
        //             //             or .security_advisory.severity == "critical")
        //             //     )
        //             // ] | length')

        //             // echo "Open HIGH/CRITICAL Dependabot alerts: ${high_critical_open_count}"

        //             // if [ "${high_critical_open_count}" -gt 0 ]; then
        //             //     echo "❌ Blocking pipeline due to OPEN HIGH/CRITICAL Dependabot alerts"
        //             //     echo "Affected dependencies:"
        //             //     echo "$response" | jq '.[] 
        //             //     | select(.state=="open" 
        //             //     and (.security_advisory.severity=="high" 
        //             //     or .security_advisory.severity=="critical"))
        //             //     | {dependency: .dependency.package.name, severity: .security_advisory.severity, advisory: .security_advisory.summary}'
        //             //     exit 1
        //             // else
        //             //     echo "✅ No OPEN HIGH/CRITICAL Dependabot alerts found"
        //             // fi
        //             // '''
        //         }
        //     }

        // }


        stage ('Build Docker image') {
            steps {
                script {
                    withAWS(region:'us-east-1',credentials:'aws-auth') {
                        sh """
                            aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                            docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                            docker images
                            docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}

                        """
                    }
                }
            }
        }

        // stage ('Trivy scan') {
        //     steps {
        //         script {
        //             sh """
        //                  trivy image \
        //                  --scanners vuln \
        //                  --severity HIGH,CRITICAL,MEDIUM \
        //                  --pkg-types os \
        //                  --exit-code 1 \
        //                  --format table \
        //                  ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
        //             """ 
        //         }
        //     }
        // }

        stage('Trigger DEV Deploy') {
            steps {
                script {
                    build job: "../${COMPONENT}-deploy",
                        wait: false, // Wait for completion
                        propagate: false, // Propagate status
                        parameters: [
                            string(name: 'appVersion', value: "${appVersion}"),
                            string(name: 'deploy_to', value: "dev")
                        ]
                }
            }
        }
    
    }
    // Post build section
    post {
        always {
            echo "Cleaning up workspace..."
            cleanWs()
        }
        success {
            echo "This build is successful. Version: ${appVersion}"
        }
    }
}

}