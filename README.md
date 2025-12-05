# piqi-evaluator

**piqi-evaluator** is a proof-of-concept application that makes use of both [piqi-map](https://github.com/navapbc/piqi-map) and [piqi-model](https://github.com/navapbc/piqi-model) to provide a straightforward way to quickly process a cohort of FHIR Patient resource bundles and evaluate them utlizing a [Patient Information Quality Improvement (PIQI)](https://build.fhir.org/ig/HL7/piqi/index.html) server.

## Creating a Cohort
The quickest way to get some data is to use [Synthea](https://github.com/synthetichealth/synthea/wiki/Basic-Setup-and-Running) to generate it:

```
java -jar synthea-with-dependencies.jar -p 100
```

That will create 100 bundles of patient data.

## Using the PIQI Alliance Reference Implementation
The PIQI Alliance has a [reference implementation](https://github.com/piqiframework/reference_application) written in C#/.NET.  It should be able to run on MacOS but strictly following the instructions in the README did not work.  

The following deviations from the README allows the reference implementation to run on MacOS.

### Modify `appsettings.json` for Terminology Server

Use the publicly available HL7 FHIR terminology server.

```
% git diff PIQI_Engine.Server/appsettings.json
diff --git a/PIQI_Engine.Server/appsettings.json b/PIQI_Engine.Server/appsettings.json
index 385fba1..df92906 100644
--- a/PIQI_Engine.Server/appsettings.json
+++ b/PIQI_Engine.Server/appsettings.json
@@ -46,7 +46,7 @@
     "ValueListPath": "ReferenceData/Values.json"
   },
   "Fhir": {
-    "BaseUrl": "<YOUR FHIR SERVER URL GOES HERE>"
+    "BaseUrl":         "http://tx.fhir.org/r4/"
   },
   "Logging": {
     "LogLevel": {

```

### Running It On MacOS
1. Download and install .NET 8.0 SDK [https://dotnet.microsoft.com/en-us/download/dotnet/thank-you/sdk-8.0.121-macos-arm64-installer]()
2. `cd reference_application/PIQI_Engine.Server`
3. `dotnet add package Microsoft.AspNetCore.SpaProxy`
4. `cd ..`
5. `dotnet restore`
6. `dotnet build`
7. `dotnet run --project PIQI_Engine.Server/PIQI_Engine.Server.csproj`
8. View Swagger documentation at: [http://localhost:5025/swagger]()
9. Test the API using `curl`

```
% curl -X 'GET' 'http://localhost:5025/Diagnostics/status' -H 'accept: text/plain' 
OK
```

### Utilizing Their Swagger JSON for API Classes
It is necessary to download the Swagger JSON schema from the reference implementation server and use it to generate the API classes needed to POST data to the server.  Use the **openapi-generator** to create the build for the Java classes and create the dependent jar file (which needs to be published to your local Maven repository - `~/.m2`).

```
% curl http://localhost:5025/swagger/v1/swagger.json > piqi-alliance-reference-implementation.schema.json
% brew install openapi-generator
% openapi-generator generate -i piqi-alliance-reference-implementation.schema.json -g java --artifact-id "reference-implementation" --artifact-version "0.1.0"  --model-package "org.piqialliance.model" --api-package "org.piqialliance.api" --group-id "org.piqialliance" -o piqi-alliance-swagger
% cd piqi-alliance-swagger
% ./gradlew build publishToMavenLocal
```


## Running the Application
**piqi-evaluator** is a Spring Boot application.  Once you have configured `application.properties` to suit your needs you can simply run it from the command line:

```
./gradlew bootRun
```


### Large Files and Timeouts
Some of the files created by Synthea are quite large and the reference implementation server takes a while to process them.  Adjust the timeouts in `application.properties` as necessary or generate the Synthea files with fewer years of data.

