@Library ('jenkins-shared-library') _

def configMap = [
    project: "roboshop",
    component: "payment",
]

if (env.BRANCH_NAME == 'main') {
    echo "Please follow the CR process"
}
else {
    pythonEKSpipeline(configMap)
}