# mockbyte (POC)
## Tool to mimic request between servers

###  PROXY
- doc: With PROXY option, it is now only use for testing and configurations.
- run-server: `./mvnw clean compile exec:java -Dexec.args="src/main/resources/httpbin.json PROXY"`
- test: `curl -X DELETE "http://localhost:4646/delete" -H "accept: application/json"`

### RECORD
- doc: Save the req/res in a direcotry.
- run-server `./mvnw clean compile exec:java -Dexec.args="src/main/resources/httpbin.json RECORD"`
- test: `curl -X DELETE "http://localhost:4646/delete" -H "accept: application/json"`

### MOCK
- doc: Use the saved file to response the request.
- run `./mvnw clean compile exec:java -Dexec.args="src/main/resources/httpbin.json MOCK"`
- test: `curl -X DELETE "http://localhost:4646/delete" -H "accept: application/json"`
