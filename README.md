Structure
=========

./docker------------------------------------------------------------------------Docker build file
./src/main/java-----------------------------------------------------------------Backend
./src/main/js-------------------------------------------------------------------Frontend
./src/main/resources------------------------------------------------------------Frontend resources
./src/test----------------------------------------------------------------------Test files

./src/main/java/sbab/ServerMain.java--------------------------------------------Server start point
./src/main/java/sbab/sl/api/SLHTTP.java-----------------------------------------Handles comunication to external API
./src/main/java/sbab/sl/api/aggregation/LinesAggregator.java--------------------Aggregates data from external API
./src/main/java/sbab/toplist/TopList.java---------------------------------------Supplies frontend
./src/main/java/sbab/toplist/TopListAPI.java------------------------------------Supplies API
./src/main/java/sbab/toplist/TopListDataContainer.java--------------------------Holds API data

Build & run instructions 
========================

Docker 2.0.0.0-mac81 was used when build this on my machine.

Replace the ACCESS_TOKEN value in the Makefile with your own token and then run following commands by first
replacing <immage_name> with something you like better then docker should build and start the server.

docker build -t <image_name> -f docker/Dockerfile .
docker run -ti -p 8081:8081 -v "$(pwd):/app" <image_name>

When you see "INFO: Server started." server should be reachable on the docker host on port 8081

Example URI: http://localhost:8081/v1/top-list
