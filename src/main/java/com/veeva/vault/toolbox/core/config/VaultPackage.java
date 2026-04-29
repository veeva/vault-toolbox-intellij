package com.veeva.vault.toolbox.core.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.veeva.vault.toolbox.core.utils.Checksum;
import com.veeva.vault.toolbox.core.models.CsvDataStep;
import com.veeva.vault.toolbox.core.models.CsvManifest;
import com.veeva.vault.toolbox.core.models.StepManifest;
import com.veeva.vault.toolbox.core.models.VpkBuildManifest;
import com.veeva.vault.toolbox.core.results.DeploymentResult;
import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.VaultModel;
import com.veeva.vault.vapil.api.model.common.Job;
import com.veeva.vault.vapil.api.model.response.*;
import com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest;
import com.veeva.vault.vapil.api.request.JobRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.veeva.vault.toolbox.core.utils.FileIO.makeDirectories;

public class VaultPackage extends VaultModel {
    private Logger logger = LoggerFactory.getLogger(VaultPackage.class);

    private final static int JOB_WAIT_SECONDS = 11;

    public final static String VAULTPACKAGE_FILENAME = "vaultpackage.xml";

    private VaultClient vaultClient;

    @JsonIgnore
    public VaultPackage(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
    }

    @JacksonXmlProperty(isAttribute = true)
    private String xmlns = "https://veevavault.com/";

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JsonGetter
    public String getName() {
        return name;
    }

    @JsonSetter
    public void setName(String name) {
        this.name = name;
    }

    @JacksonXmlProperty(localName = "source")
    private Source source;

    @JsonGetter
    public Source getSource() {
        return source;
    }

    @JsonSetter
    public void setSource(Source source) {
        this.source = source;
    }

    public enum PackageType {
        MIGRATION("migration__v"),
        TESTDATA("test_data__sys");

        String value;
        PackageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @JacksonXmlProperty(localName = "packagetype")
    private String packageType = "migration__v";

    @JsonGetter
    public String getPackageType() {
        return packageType;
    }

    @JsonAnySetter
    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    @JsonIgnore
    public void setPackageType(PackageType packageType) {
        this.packageType = packageType.getValue();
    }

    @JacksonXmlProperty(localName = "summary")
    private String summary;

    @JsonGetter
    public String getSummary() {
        return summary;
    }

    @JsonSetter
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @JacksonXmlProperty(localName = "description")
    private String description;

    @JsonGetter
    public String getDescription() {
        return description;
    }

    @JsonSetter
    public void setDescription(String description) {
        this.description = description;
    }

    @JacksonXmlProperty(localName = "javasdk")
    private JavaSdk javaSdk;

    @JsonGetter
    public JavaSdk getJavaSdk() {
        return javaSdk;
    }

    @JsonSetter
    public void setJavaSdk(JavaSdk javaSdk) {

        this.javaSdk = javaSdk;
    }

    @JsonIgnore
    public void importToVault(VaultClient vaultClient, File packageFile) throws IOException {
        JobCreateResponse jobCreateResponse = vaultClient.newRequest(ConfigurationMigrationRequest.class)
                .importPackage();
    }

    @JsonIgnore
    public File createXmlFile(File outputDir) {
        File xmlFileName = new File(outputDir, VAULTPACKAGE_FILENAME);
        makeDirectories(outputDir);

        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
            xmlMapper.writeValue(xmlFileName, this);

            return xmlFileName;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @JsonIgnore
    public void pack(File sourceDirectory, File packageFile) {
        try {
            prepVpkFiles(sourceDirectory);
            List<File> files = getVpkFiles(sourceDirectory);
            for (File file : files) {
                System.out.println(file.getAbsolutePath());
            }
            FileIO.zipFiles(packageFile, getVpkFiles(sourceDirectory), sourceDirectory, null, false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JsonIgnore
    public List<File> getVpkFiles(File directory) {
        try (Stream<Path> walk = Files.walk(directory.toPath())) {
            Set<String> fileExtensions = new HashSet<>();
            fileExtensions.add(".csv");
            fileExtensions.add(".dep");
            fileExtensions.add(".java");
            fileExtensions.add(".json");
            fileExtensions.add(".mdl");
            fileExtensions.add(".md5");
            fileExtensions.add(".xml");
            fileExtensions.add(".js");
            fileExtensions.add(".css");
            fileExtensions.add(".png");
            fileExtensions.add(".jpg");
            return FileIO.getFiles(directory,fileExtensions);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private JobStatusResponse getJobStatusWithRetry(int jobId, Consumer<ProgressResult> progressConsumer, String jobType) {
        try {
            Thread.sleep(1000);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
        JobStatusResponse jobStatusResponse = null;
        int tries = 0;
        boolean completed = false;
        while (!completed) {
            if (tries > 0) {
                try {
                    int numSeconds = JOB_WAIT_SECONDS;
                    for (int i = 0; i < numSeconds; i++) {
                        progressConsumer.accept(new ProgressResult("Waiting " + (numSeconds - i) + " seconds for " + jobType + " Job ID = " + jobId));
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            progressConsumer.accept(new ProgressResult("Checking job status for Job ID = " + jobId));
            jobStatusResponse = vaultClient.newRequest(JobRequest.class)
                    .retrieveJobStatus(jobId);
            tries++;

            if (jobStatusResponse.getData().getStatus().equals("SUCCESS")
                    || jobStatusResponse.getData().getStatus().equals("FAILED")
                    || jobStatusResponse.getData().getStatus().equals("ERRORS_ENCOUNTERED")
                    || jobStatusResponse.getData().getStatus().equals("COMPLETED")) {
                completed = true;
            }
            else if (tries == 20) {
                completed = true;
            }
        }

        return jobStatusResponse;
    }

    @JsonIgnore
    public DeploymentResult deployPackage(File packageFile, Consumer<ProgressResult> progressConsumer) {
        DeploymentResult deploymentResult = new DeploymentResult();
        try {
            progressConsumer.accept(new ProgressResult("Preparing Deployment"));

            JobCreateResponse importJobResponse = vaultClient.newRequest(ConfigurationMigrationRequest.class)
                    .setInputPath(packageFile.getAbsolutePath())
                    .importPackage();

            if (importJobResponse.isFailure() || importJobResponse.getJobId() == null) {
                String errorMsg = importJobResponse.getResponseMessage();
                if (importJobResponse.getErrors() != null && !importJobResponse.getErrors().isEmpty()) {
                    errorMsg += " - " + importJobResponse.getErrors().get(0).getMessage();
                }
                deploymentResult.addErrorMessage("Vault rejected package import: " + errorMsg);
                return deploymentResult;
            }

            JobStatusResponse jobStatusResponse = getJobStatusWithRetry(importJobResponse.getJobId(), progressConsumer, "Import");

            String jobStatus = jobStatusResponse.getData().getStatus();
            if (jobStatus.equals("SUCCESS") || jobStatus.equals("ERRORS_ENCOUNTERED") || jobStatus.equals("FAILED") || jobStatus.equals("COMPLETED")) {
                progressConsumer.accept(new ProgressResult("Checking Deployment Status"));

                Job.Link packageLink = jobStatusResponse.getData().getLinks()
                        .stream()
                        .filter(link -> link.getRel().equals("artifacts"))
                        .findFirst().orElse(null);

                if (packageLink != null) {
                    String packageId = packageLink.getHref()
                            .substring(packageLink.getHref().lastIndexOf("/vault_package__v/") + 18,
                                    packageLink.getHref().lastIndexOf("/actions/import/results"));

                    PackageImportResultsResponse packageImportResultsResponse = vaultClient
                            .newRequest(ConfigurationMigrationRequest.class)
                            .retrievePackageImportResults(packageId);

                    if (packageImportResultsResponse.isSuccessful()) {
                        progressConsumer.accept(new ProgressResult("Imported Package"));

                        if (packageImportResultsResponse.getVaultPackage()
                                .getPackageStatus().equals("imported__v")
                                || packageImportResultsResponse.getVaultPackage()
                                .getPackageStatus().equals("verified__v")) {

                            progressConsumer.accept(new ProgressResult("Deploying Package"));

                            JobCreateResponse deployPackageResponse = vaultClient
                                    .newRequest(ConfigurationMigrationRequest.class)
                                    .deployPackage(packageId);

                            if (deployPackageResponse.isFailure() || deployPackageResponse.getJobId() == null) {
                                String errorMsg = deployPackageResponse.getResponseMessage();
                                if (deployPackageResponse.getErrors() != null && !deployPackageResponse.getErrors().isEmpty()) {
                                    errorMsg += " - " + deployPackageResponse.getErrors().get(0).getMessage();
                                }
                                deploymentResult.addErrorMessage("Vault rejected package deployment: " + errorMsg);
                                return deploymentResult;
                            }

                            jobStatusResponse = getJobStatusWithRetry(deployPackageResponse.getJobId(), progressConsumer, "Deployment");

                            String deployJobStatus = jobStatusResponse.getData().getStatus();
                            if (deployJobStatus.equals("SUCCESS") || deployJobStatus.equals("ERRORS_ENCOUNTERED") || deployJobStatus.equals("FAILED") || deployJobStatus.equals("COMPLETED")) {

                                PackageDeploymentResultsResponse packageDeploymentResultsResponse = vaultClient.newRequest(ConfigurationMigrationRequest.class).retrievePackageDeployResults(packageId);
                                com.veeva.vault.vapil.api.model.response.PackageDeploymentResultsResponse.ResponseDetails details = packageDeploymentResultsResponse.getResponseDetails();
                                String status = details.getPackageStatus();

                                String packageName = packageFile.getName()
                                        .replaceAll("(?i)\\.vpk$", "")
                                        .replaceAll("(?i)\\.zip$", "");

                                boolean hasErrors = false;
                                boolean hasWarnings = false;
                                String statusSuffix = "finished with status: " + status;

                                if ("deployed__v".equalsIgnoreCase(status)) {
                                    statusSuffix = "been successfully deployed";
                                } else if ("failed__v".equalsIgnoreCase(status)) {
                                    statusSuffix = "failed to deploy";
                                    hasErrors = true;
                                } else if ("verified__v".equalsIgnoreCase(status)) {
                                    statusSuffix = "been successfully verified";
                                } else if ("warnings_encountered__v".equalsIgnoreCase(status)) {
                                    statusSuffix = "been deployed with warnings";
                                    hasWarnings = true;
                                } else if ("error".equalsIgnoreCase(status) || "error__v".equalsIgnoreCase(status) || "errors_encountered__v".equalsIgnoreCase(status)) {
                                    statusSuffix = "encountered an error";
                                    hasErrors = true;
                                } else if (status != null) {
                                    statusSuffix = "finished with status: " + status.replace("__v", "").replace("_", " ");
                                    if (status.toLowerCase().contains("error") || status.toLowerCase().contains("fail")) {
                                        hasErrors = true;
                                    }
                                }

                                String friendlyMessage = "Package " + packageName + " has " + statusSuffix + ".";
                                progressConsumer.accept(new ProgressResult(friendlyMessage));

                                Object deployedObj = details.get("deployed");
                                Object failedObj = details.get("failed");
                                Object warningsObj = details.get("deployed_with_warnings");

                                String deployedCount = deployedObj != null ? String.valueOf(deployedObj) : "0";
                                String failedCount = failedObj != null ? String.valueOf(failedObj) : "0";
                                String warningsCount = warningsObj != null ? String.valueOf(warningsObj) : "0";

                                if (!"0".equals(failedCount)) hasErrors = true;
                                if (!"0".equals(warningsCount)) hasWarnings = true;

                                String summaryStr = "Summary: " + deployedCount + " Deployed, " + failedCount + " Failed, " + warningsCount + " Warnings";

                                if (hasErrors) {
                                    deploymentResult.addErrorMessage(friendlyMessage);
                                    deploymentResult.addErrorMessage(summaryStr);
                                } else if (hasWarnings) {
                                    deploymentResult.addWarnMessage(friendlyMessage);
                                    deploymentResult.addWarnMessage(summaryStr);
                                } else {
                                    deploymentResult.addInfoMessage(friendlyMessage);
                                    deploymentResult.addInfoMessage(summaryStr);
                                }

                                Object rawLogs = details.get("deployment_log");
                                if (rawLogs instanceof java.util.List && !((java.util.List<?>) rawLogs).isEmpty()) {
                                    String logHeader = "\nVault Deployment Logs:";
                                    if (hasErrors) deploymentResult.addErrorMessage(logHeader);
                                    else if (hasWarnings) deploymentResult.addWarnMessage(logHeader);
                                    else deploymentResult.addInfoMessage(logHeader);

                                    for (Object rawLog : (java.util.List<?>) rawLogs) {
                                        String filename = null;
                                        if (rawLog instanceof com.veeva.vault.vapil.api.model.VaultModel) {
                                            filename = (String) ((com.veeva.vault.vapil.api.model.VaultModel) rawLog).get("filename");
                                        } else if (rawLog instanceof java.util.Map) {
                                            filename = (String) ((java.util.Map<?, ?>) rawLog).get("filename");
                                        }

                                        if (filename != null) {
                                            String logBullet = " • " + filename;
                                            if (hasErrors) deploymentResult.addErrorMessage(logBullet);
                                            else if (hasWarnings) deploymentResult.addWarnMessage(logBullet);
                                            else deploymentResult.addInfoMessage(logBullet);
                                        }
                                    }
                                }

                                Object rawSteps = details.get("package_steps");
                                if (rawSteps instanceof java.util.List) {
                                    for (Object rawStep : (java.util.List<?>) rawSteps) {
                                        Object rawComponents = null;

                                        if (rawStep instanceof com.veeva.vault.vapil.api.model.VaultModel) {
                                            rawComponents = ((com.veeva.vault.vapil.api.model.VaultModel) rawStep).get("package_components");
                                        } else if (rawStep instanceof java.util.Map) {
                                            rawComponents = ((java.util.Map<?, ?>) rawStep).get("package_components");
                                        }

                                        if (rawComponents instanceof java.util.List) {
                                            for (Object rawComp : (java.util.List<?>) rawComponents) {
                                                String compStatus = null, compMessage = null, compName = null, compType = null;

                                                if (rawComp instanceof com.veeva.vault.vapil.api.model.VaultModel) {
                                                    com.veeva.vault.vapil.api.model.VaultModel vm = (com.veeva.vault.vapil.api.model.VaultModel) rawComp;
                                                    compStatus = (String) vm.get("status");
                                                    compMessage = (String) vm.get("response_message");
                                                    compName = (String) vm.get("name");
                                                    compType = (String) vm.get("type");
                                                } else if (rawComp instanceof java.util.Map) {
                                                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) rawComp;
                                                    compStatus = (String) map.get("status");
                                                    compMessage = (String) map.get("response_message");
                                                    compName = (String) map.get("name");
                                                    compType = (String) map.get("type");
                                                }

                                                if (compMessage != null && !compMessage.isEmpty() && !"SUCCESS".equalsIgnoreCase(compMessage)) {
                                                    String detailedMsg = compName + " (" + compType + "): " + compMessage;

                                                    if ("failed__v".equalsIgnoreCase(compStatus)) {
                                                        deploymentResult.addErrorMessage(detailedMsg);
                                                    } else if ("warnings_encountered__v".equalsIgnoreCase(compStatus) || compMessage.toLowerCase().contains("warning")) {
                                                        deploymentResult.addWarnMessage(detailedMsg);
                                                    } else {
                                                        deploymentResult.addInfoMessage(detailedMsg);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            deploymentResult.addErrorMessage("Package verification failed with status: " + packageImportResultsResponse.getVaultPackage().getPackageStatus());
                        }
                    } else {
                        deploymentResult.addErrorMessage(packageImportResultsResponse.getResponseMessage());
                    }
                } else {
                    deploymentResult.addErrorMessage("Vault job completed with status '" + jobStatus + "' but returned no artifact logs. The package may be entirely empty or invalid.");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            deploymentResult.addErrorMessage("Exception during deployment: " + e.getMessage());
        }
        return deploymentResult;
    }

    public static class JavaSdk extends VaultModel {
        public enum DeploymentOption {
            NONE("none"),
            DELETE_ALL("delete_all"),
            INCREMENTAL("incremental"),
            REPLACE_ALL("replace_all");

            String value;
            DeploymentOption(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }

        @JsonIgnore
        public JavaSdk() {

        }

        @JsonIgnore
        public JavaSdk(DeploymentOption deploymentOption) {
            this.setDeploymentOption(deploymentOption);
        }

        @JacksonXmlProperty(localName = "deployment_option")
        private String deploymentOption;

        @JsonGetter
        public String getDeploymentOption() {
            return deploymentOption;
        }

        @JsonAnySetter
        public JavaSdk setDeploymentOption(String deploymentOption) {
            this.deploymentOption = deploymentOption;
            return this;
        }

        @JsonIgnore
        public void setDeploymentOption(DeploymentOption deploymentOption) {
            this.deploymentOption = deploymentOption.getValue();
        }
    }

    public static class Source extends VaultModel {

        @JacksonXmlProperty(localName = "vault")
        private Integer vault;

        @JacksonXmlProperty(localName = "author")
        private String author;

        @JsonGetter
        public Integer getVault() {
            return vault;
        }

        @JsonSetter
        public void setVault(Integer vault) {
            this.vault = vault;
        }

        @JsonGetter
        public String getAuthor() {
            return author;
        }

        @JsonSetter
        public void setAuthor(String author) {
            this.author = author;
        }

    }

    public void prepVpkFiles(File directory) {
        try {
            List<File> files = getVpkFiles(directory);

            for (File componentFile : files) {
                String componentName = componentFile.getName().substring(0, componentFile.getName().lastIndexOf("."));
                String md5 = Checksum.getMd5(componentFile);

                if (componentFile.getName().toLowerCase().endsWith(".mdl")) {
                    String componentContent = new String(Files.readAllBytes(componentFile.toPath()), StandardCharsets.UTF_8);
                    boolean multiMdl = isMultiMDL(componentContent);

                    if (multiMdl) {
                        String md5FilePath = componentFile.getParent() + File.separator + componentName + ".md5";
                        File md5File = new File(md5FilePath);
                        if (md5File != null && md5File.exists()) {
                            md5File.delete();
                        }

                        boolean hasChange = false;
                        StepManifest stepManifest;
                        String xmlFilePath = componentFile.getAbsolutePath().substring(0, componentFile.getAbsolutePath().lastIndexOf(".")) + ".xml";
                        logger.info(xmlFilePath);
                        File xmlFile = new File(xmlFilePath);
                        if (xmlFile.exists()) {
                            String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
                            ObjectMapper objectMapper = new XmlMapper();
                            stepManifest = objectMapper.readValue(xmlContent, StepManifest.class);
                        }
                        else {
                            stepManifest = new StepManifest();
                            stepManifest.setLabel(componentName);
                        }

                        if (!md5.equals(stepManifest.getChecksum())) {
                            stepManifest.setChecksum(md5);
                            hasChange = true;
                        }

                        if (hasChange) {
                            logger.info("RECREATE: " + xmlFile);
                            XmlMapper xmlMapper = new XmlMapper();
                            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
                            xmlMapper.writeValue(xmlFile, stepManifest);
                        }
                        else {
                            logger.info("VALID: " + xmlFile);
                        }
                    }
                    else {
                        String md5FilePath = componentFile.getParent() + File.separator + componentName + ".md5";
                        File md5File = new File(md5FilePath);
                        String validMd5Content = md5 + " " + componentName;

                        if (md5File.exists()) {
                            String existingMd5Content = new String(Files.readAllBytes(md5File.toPath()), StandardCharsets.UTF_8);
                            if ((existingMd5Content == null) || (!existingMd5Content.equals(validMd5Content))) {
                                logger.info("RECREATE: " + md5FilePath);
                                FileIO.writeFileContent(Paths.get(md5FilePath).toFile(), validMd5Content);
                            } else {
                                logger.info("VALID: " + md5FilePath);
                            }
                        } else {
                            logger.info("CREATE: " + md5FilePath);
                            FileIO.writeFileContent(Paths.get(md5FilePath).toFile(), validMd5Content);
                        }
                    }
                }
                else if (componentFile.getName().toLowerCase().endsWith(".csv")) {
                    boolean hasChange = false;
                    CsvManifest csvManifest;
                    String xmlFilePath = componentFile.getAbsolutePath().substring(0, componentFile.getAbsolutePath().lastIndexOf(".")) + ".xml";
                    File xmlFile = new File(xmlFilePath);
                    if (xmlFile.exists()) {
                        String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);

                        ObjectMapper objectMapper = new XmlMapper();
                        csvManifest = objectMapper.readValue(xmlContent, CsvManifest.class);

                    }
                    else {
                        csvManifest = new CsvManifest();
                        csvManifest.setLabel(componentFile.getName());
                        CsvDataStep csvDataStep = new CsvDataStep();
                        csvManifest.setCsvDataStep(csvDataStep);
                    }


                    if (!md5.equals(csvManifest.getChecksum())) {
                        csvManifest.setChecksum(md5);
                        hasChange = true;
                    }

                    CsvDataStep csvDataStep = csvManifest.getCsvDataStep();
                    if (csvDataStep != null) {
                        int rowCount = FileIO.getCsvRowCount(componentFile) - 1;
                        if ((csvDataStep.getRecordCount() == null) ||
                                (csvDataStep.getRecordCount() != rowCount)) {
                            csvDataStep.setRecordCount(rowCount);
                            hasChange = true;
                        }
                    }

                    if (hasChange) {
                        XmlMapper xmlMapper = new XmlMapper();
                        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
                        xmlMapper.writeValue(xmlFile, csvManifest);
                    }
                }
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

    }

    public boolean isMultiMDL(String mdl) {
        if (mdl != null) {
            String testMdl = ";" + StringUtils.normalizeSpace(mdl).replace(" ", "");
            int commandCount = 0;
            for (MdlResponse.CommandType commandType : MdlResponse.CommandType.values()) {
                commandCount += StringUtils.countMatches(testMdl, ";" + commandType.getValue());
            }
            return (commandCount > 1);
        }
        return false;
    }

    public File buildFromManifest(File buildManifestFile,
                                  File workingDirectory,
                                  File relativePath,
                                  String username,
                                  Integer vaultId) {
        VpkBuildManifest buildManifest = VpkBuildManifest.load(buildManifestFile);

        File buildDirectory = new File(workingDirectory.getPath(), "build");
        if (buildDirectory.exists()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(buildDirectory);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        makeDirectories(buildDirectory);

        this.setName(buildManifest.getName());
        if (this.getSource() == null) {
            this.setSource(new Source());
        }
        this.getSource().setVault(vaultId);

        if (buildManifest.getAuthor() != null && !buildManifest.getAuthor().isEmpty()) {
            this.getSource().setAuthor(buildManifest.getAuthor());
        } else {
            this.getSource().setAuthor(username);
        }

        VpkBuildManifest.JavaSdk buildManifestJavaSdk = buildManifest.getJavaSdk();
        if (buildManifestJavaSdk != null) {
            this.setJavaSdk(new JavaSdk());
            this.getJavaSdk().setDeploymentOption(buildManifestJavaSdk.getDeploymentOption());

            String local = buildManifest.getJavaSdk().getPath();
            local = local.substring(local.lastIndexOf("src/main/java/com/veeva/vault/custom"));
            File javaSdkBuildDirectory = new File(buildDirectory.getPath(), "javasdk/" + local);
            copyFiles(new File(relativePath.getPath(), buildManifest.getJavaSdk().getPath()), javaSdkBuildDirectory);
        }

        VpkBuildManifest.WebSdk webSdk = buildManifest.getWebSdk();
        if (webSdk != null) {
            if (buildManifestJavaSdk == null) {
                this.setJavaSdk(new JavaSdk());
                this.getJavaSdk().setDeploymentOption(JavaSdk.DeploymentOption.INCREMENTAL);
            }

            for (VpkBuildManifest.WebSdk.Distribution distribution : webSdk.getDistributions()) {

                File shell = new File(relativePath.getPath(), distribution.getShell());
                if (shell.exists()) {
                    try {
                        ProcessBuilder pb = new ProcessBuilder("sh", shell.getName());
                        pb.directory(shell.getParentFile());
                        final Process process = pb.start();
                    }
                    catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }


                File distributionDirectory = new File(buildDirectory.getPath(), "websdk/" + distribution.getName());
                makeDirectories(distributionDirectory);

                File distSource = new File(relativePath.getPath(), distribution.getPath());
                File distTarget = new File(distributionDirectory.getPath(), "dist");
                copyFiles(distSource, distTarget);

                File manifestSource = new File(relativePath.getPath(), distribution.getManifest());
                File manifestTarget = new File(distributionDirectory.getPath(), "distribution-manifest.json");
                copyFiles(manifestSource, manifestTarget);
            }
        }

        this.setPackageType(buildManifest.getPackageType());
        this.setDescription(buildManifest.getDescription());
        this.setSummary(buildManifest.getSummary());

        List<VpkBuildManifest.Component> components = buildManifest.getComponents();
        if (components != null) {
            for (VpkBuildManifest.Component component : components) {
                File sourceFile = new File(relativePath.getPath(), component.getPath());
                File targetFile = new File(buildDirectory.getPath(), "components/" + component.getStep() + "/" + sourceFile.getName());
                copyFiles(sourceFile,targetFile);

                if (sourceFile.getName().toLowerCase().endsWith(".csv")) {
                    String xmlPath = sourceFile.getAbsolutePath().substring(0, sourceFile.getAbsolutePath().lastIndexOf(".")) + ".xml";
                    File xmlSourceFile = new File(xmlPath);
                    if (xmlSourceFile.exists()) {
                        File xmlTargetFile = new File(targetFile.getParent(), xmlSourceFile.getName());
                        copyFiles(xmlSourceFile, xmlTargetFile);
                    }
                }
            }
        }

        this.createXmlFile(buildDirectory);
        File vpkFile = new File(workingDirectory.getPath(), "packages/" + buildManifest.getName() + ".vpk");
        this.pack(buildDirectory, vpkFile);

        return vpkFile;
    }

    private void copyFiles(File source, File target) {
        if (source.isDirectory()) {
            makeDirectories(target);
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyFiles(file, new File(target, file.getName()));
                }
            }
        }
        else {
            try {
                makeDirectories(target.getParentFile());
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}