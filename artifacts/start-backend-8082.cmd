@echo off
cd /d F:\dachuang\backend
echo [%date% %time%] starting backend on 8082 > F:\dachuang\artifacts\backend-8082.log
mvn -s F:\dachuang\maven-settings.xml -Dmaven.repo.local=F:/dachuang/.m2/repository spring-boot:run >> F:\dachuang\artifacts\backend-8082.log 2>&1
