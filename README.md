# mockbyte (POC)
## Tool to mock request between servers

###  PROXY
- doc: With PROXY option, it is now only use for testing and configurations.
- run-server: `./mvnw clean compile exec:java -Dexec.args="src/main/resources/httpbin.json http proxy"`
- test: `curl -X DELETE "http://localhost:4646/delete" -H "accept: application/json"`

### RECORD
- doc: Save the req/res in a directory.
- run-server `./mvnw clean compile exec:java -Dexec.args="src/main/resources/config.json http record"`
- test: `curl -X DELETE "http://localhost:4646/delete" -H "accept: application/json"`

### MOCK
- doc: Use the saved file to response the request.
- run-server `./mvnw clean compile exec:java -Dexec.args="src/main/resources/config.json http mock"`
- test: `curl -X DELETE "http://localhost:4646/delete" -H "accept: application/json"`
