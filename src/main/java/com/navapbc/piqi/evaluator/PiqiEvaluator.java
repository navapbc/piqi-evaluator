package com.navapbc.piqi.evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.navapbc.piqi.map.fhir.PiqiBaseMapper;
import com.navapbc.piqi.map.fhir.PiqiBaseR4Mapper;
import com.navapbc.piqi.model.PiqiDemographics;
import com.navapbc.piqi.model.PiqiLabResult;
import com.navapbc.piqi.model.PiqiPatient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.piqialliace.model.DataClassScoreResult;
import org.piqialliace.model.PIQIRequest;
import org.piqialliace.model.PIQIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class PiqiEvaluator implements CommandLineRunner {

    @Value("${com.nava.piqi.evaluator.input}")
    private String inputFileOrDirectory;

    @Value("${com.nava.piqi.evaluator.input.type}")
    private String inputType;

    @Value("${com.nava.piqi.evaluator.server.url}")
    private String serverUrl;

    @Value("${com.nava.piqi.evaluator.outputDirectory}")
    private String outputDirectory;

    @Autowired
    private Set<PiqiBaseMapper> mappers;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    private final PiqiScorecard piqiScorecard = new PiqiScorecard();


    @Override
    public void run(String... args) throws Exception {
        log.info("Starting PiqiEvaluator...");
        List<String> inputFiles = getFilesToEvaluate();
        String outputPath = createOutputDirectory();
        log.info("Output path for this run=[{}]", outputPath);
        //mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        long fileCounter = 0;
        if (!inputFiles.isEmpty()) {
            for (String inputFile : inputFiles) {
                fileCounter++;
                log.info("Processing file {} of {} = [{}]", fileCounter, inputFiles.size(), inputFile);
                piqiScorecard.addToNumberOfFilesProcessed(1L);
                PiqiPatient piqiPatient;
                try {
                    Bundle parsed = parser.parseResource(Bundle.class, Files.readString(Paths.get(inputFile)));
                    log.debug("parsed=[{}]", parsed.getEntry().size());
                    PiqiBaseR4Mapper piqiBaseR4Mapper = (PiqiBaseR4Mapper) getMapper(FhirVersionEnum.R4, PiqiDemographics.class);
                    piqiPatient = new PiqiPatient();
                    piqiPatient.setDemographics(piqiBaseR4Mapper.mapDemographics(parsed));
                    piqiBaseR4Mapper = (PiqiBaseR4Mapper) getMapper(FhirVersionEnum.R4, PiqiLabResult.class);
                    piqiPatient.setLabResults(piqiBaseR4Mapper.mapLabResults(parsed));
                    log.debug("Patient as json = {}", mapper.writeValueAsString(piqiPatient));
                    NavaPiqiMessageData piqiMessageData = new NavaPiqiMessageData();
                    piqiMessageData.setFormatId("FHIR_JSON");
                    piqiMessageData.setMessageId("Msg001");
                    piqiMessageData.setPatient(piqiPatient);
                    PIQIRequest piqiRequest = new PIQIRequest();
                    piqiRequest.setDataSourceID("TestSource");
                    piqiRequest.setDataProviderID("TestDataProvider");
                    piqiRequest.setEvaluationRubricMnemonic("USCDI_V3");
                    piqiRequest.setPiqiModelMnemonic("PAT_CLINICAL_V1");
                    piqiRequest.setMessageID("Msg001");
                    piqiRequest.setMessageData(mapper.writeValueAsString(piqiMessageData));
//        piqiRequest.setMessageData(Files.readString(Paths.get("labresults-valid.json")));
//        piqiRequest.setMessageData(Files.readString(Paths.get("Test1_PIQI.json")));
//        piqiRequest.setMessageData(Files.readString(Paths.get("demographics-invalid.json")));
                    ResponseEntity<PIQIResponse> result = postToPiqiServer(mapper.writeValueAsString(piqiRequest));
                    updatePiqiScorecard(result, inputFile,  outputPath);
                } catch (Exception e) {
                    log.error("Error processing file=[{}]", inputFile, e);
                }
            }
        } else {
            log.warn("No input files were provided");
        }
        FileWriter writer = new FileWriter(outputPath + File.separator + "piqi-scorecard.json");
        writer.write(mapper.writeValueAsString(piqiScorecard));
        writer.close();
        log.info("...Ending PiqiEvaluator.");
    }

    private void updatePiqiScorecard(ResponseEntity<PIQIResponse> result, String inputFile, String outputPath) throws IOException {
        log.info("Updating PiqiScorecard...");
        // Log results
        if (result != null && !result.getStatusCode().isError()) {
            PIQIResponse response = result.getBody();
            if (response != null && response.getSucceeded() != null && response.getSucceeded()) {
                piqiScorecard.addToSuccess(1L);
                if (response.getScoringData() != null && response.getScoringData().getMessageResults() != null && response.getScoringData().getMessageResults().getPiqiScore() != null) {
                    piqiScorecard.addToAccumulatedScore(Long.valueOf(response.getScoringData().getMessageResults().getPiqiScore()));
                }
                if (response.getScoringData() != null && response.getScoringData().getDataClassResults() != null) {
                    for (DataClassScoreResult dataClassScoreResult : response.getScoringData().getDataClassResults()) {
                        piqiScorecard.addToCategoryScore(dataClassScoreResult);
                    }
                }
            } else {
                piqiScorecard.addToFailed(1L);
            }
            String fileName = outputPath + File.separator + Paths.get(inputFile).getFileName().toString() + ".piqi";
            log.debug("Output file=[{}]", fileName);
            FileWriter writer = new FileWriter(fileName);
            writer.write(mapper.writeValueAsString(response));
            writer.close();
        } else {
            log.error("Error posting file=[{}]", inputFile);
            piqiScorecard.addToFailed(1L);
        }
    }

    private List<String> getFilesToEvaluate() throws IOException {
        List<String> canonicalFileNames = new ArrayList<>();
        if (inputFileOrDirectory != null && !inputFileOrDirectory.isEmpty()) {
            File f = new File(inputFileOrDirectory);
            if (f.exists()) {
                if (!f.isDirectory()) {
                    canonicalFileNames.add(f.getCanonicalPath());
                } else {
                    File[] contents = f.listFiles();
                    if (contents != null) {
                        for (File content : contents) {
                            canonicalFileNames.add(content.getCanonicalPath());
                        }
                    }
                }
            }
        }
        return canonicalFileNames;
    }

    private PiqiBaseMapper getMapper(FhirVersionEnum fhirVersion, Class<?> piqiClass) {
        PiqiBaseMapper mapper = null;
        for (PiqiBaseMapper piqiBaseMapper : mappers) {
            if (piqiBaseMapper.isFhirVersion(fhirVersion) && piqiBaseMapper.isMappingClassFor(piqiClass)) {
                mapper = piqiBaseMapper;
                break;
            }
        }
        return mapper;
    }

    private ResponseEntity<PIQIResponse> postToPiqiServer(String json) {
        ResponseEntity<PIQIResponse> result = null;
        // POST the contents of the payload file to ReportStream.
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
        requestHeaders.setBasicAuth("TestUser", "BlueElephantSunshinePizza!1");
        HttpEntity<String> requestEntity = new HttpEntity<>(json, requestHeaders);
        try {
            result = restTemplate.postForEntity(serverUrl, requestEntity, PIQIResponse.class);
            log.info("POST result status: {}", result.getStatusCode());
            //log.info("Result body=[{}]", result.getBody());
        } catch (Exception e) {
            log.error("Error while posting [{}]", json, e);
        }
        return result;
    }

    private String createOutputDirectory() throws IOException {
        String outputDirectoryForRun;
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                log.error("Could not create output parent directory [{}]", outputDirectory);
                throw new RuntimeException("Could not create output parent directory [" + outputDirectory + "]");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String dateTimeString = now.format(formatter);
        dir = new File(outputDirectory + File.separator + dateTimeString);
        if (!dir.mkdirs()) {
            log.error("Could not create output runtime directory [{}]", outputDirectory);
            throw new RuntimeException("Could not create output runtime directory [" + outputDirectory + "]");
        } else {
            outputDirectoryForRun = dir.getCanonicalPath();
        }

        return outputDirectoryForRun;
    }
}
