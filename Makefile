BUILD_JAR=build/libs/sbab.jar
ACCESS_TOKEN=no_token_yet

.PHONY: clean_run
clean_run: clean ${BUILD_JAR}
	@java -Dtop.list.data.access.token=${ACCESS_TOKEN} -jar ${BUILD_JAR}

.PHONY: assemble
assemble:
	./gradlew assemble

${BUILD_JAR}: assemble build_js
	./gradlew jar 
	cp $@ .

node_modules:
	npm install

.PHONY: build_js
build_js: node_modules
	mkdir -p build/resources/main/top-list/res
	`npm bin babel`/babel src/main/js -d build/resources/main/top-list/res

.PHONY: clean
clean:
	./gradlew clean

