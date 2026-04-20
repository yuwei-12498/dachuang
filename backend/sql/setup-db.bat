@echo off
if "%MYSQL_PWD%"=="" (
  echo Please set MYSQL_PWD before running this script.
  exit /b 1
)
mysql -u root --default-character-set=utf8mb4 -e "USE city_trip_db; source f:/dachuang/backend/sql/init.sql; source f:/dachuang/backend/sql/seed_poi_web_20260408.sql; source f:/dachuang/backend/sql/refresh_poi_web_20260408.sql;"
