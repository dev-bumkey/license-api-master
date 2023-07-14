### DEV Server

deploy jar path : /app/cocktail-java

    #deploy
    mv /home/ubuntu/cocktail-api-0.0.1-SNAPSHOT.jar /app/cocktail-java/
    docker restart cocktail-java

docker run -d --restart=always -p 8080:8080 --name=cocktail-java -v /app/log/cocktail-java:/var/log/cocktail -v "$PWD":/app/cocktail-java java:8 java -Dspring.profiles.active="dev" -jar /app/cocktail-java/cocktail-api-0.0.1-SNAPSHOT.jar

URL : http://52.79.126.133:8080

http://52.79.126.133:8080/api/services

DB :  52.78.107.226:3306  user->cocktail pwd->C0ckt@il

<s>### Worker</s>

<s>설치 스크립트 : wget -O - http://{cocktail-external-host}:{cocktail-external-port}/api/worker/workerinstaller/{provider}/{os}/{cluster-id}|bash</s>

<s>예 : wget -O - http://localhost:8080/api/worker/workerinstaller/aws/ubuntu/cluster_id_01|bash</s>

<s>cocktail-external-host , cocktail-external-port -> cocktail-core - run.acloud.framework.properties.WorkerProperties</s> 
