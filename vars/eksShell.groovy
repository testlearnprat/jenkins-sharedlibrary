def call(String registryCred = 'a', String registryname = 'a', String docTag = 'a', String grepo = 'a', String gbranch = 'a', String gitcred = 'a', String depname = 'a', String contname = 'a') {

pipeline {
environment { 
		registryCredential = "${registryCred}"
    		registry = "${registryname}" 	
		dockerTag = "${docTag}$BUILD_NUMBER"
		gitRepo = "${grepo}"
		gitBranch = "${gbranch}"
		gitCredId = "${gitcred}"
    		deployment = "${depname}"
    		containerName = "${contname}"
	}
		
    agent none

    stages {
        stage("POLL SCM"){
		agent{label 'docker'}
            		steps {
                	checkout([$class: 'GitSCM', branches: [[name: "$gitBranch"]], extensions: [], userRemoteConfigs: [[credentialsId: "$gitCredId", url: "$gitRepo"]]])             
            		}
        } 
        
        stage('BUILD IMAGE') {
		agent{label 'docker'}
            		steps {
                	sh 'docker build -t $registry:$dockerTag .'             
            		}
        }
        stage('SonarQube Analysis') {
		agent{label 'sonaeqube'}
			    steps{
			    withSonarQubeEnv('sonartest') {
				    sonar-scanner \
  -Dsonar.projectKey=sonartest \
  -Dsonar.sources=. \
  -Dsonar.host.url=http://65.2.34.47:9000 \
  -Dsonar.login=sqp_74b9dd28e735749035234b885b3be012562e5c46
			    }
		    }
	    }
        stage('PUSH HUB') { 
		agent{label 'docker'}
            		steps {
			            sh 'docker push $registry:$dockerTag'                   	
                	}    
        }
        
        stage('DEPLOY IMAGE') {
		agent{label 'eks'}
		          steps {
			            sh 'kubectl set image deploy $deployment $containerName="$registry:$dockerTag" --record'
		          }
	}  
    }
}  
}
