package com.veeva.vault.toolbox.core.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.veeva.vault.toolbox.core.models.CsvDataStep;
import com.veeva.vault.toolbox.core.models.CsvManifest;
import com.veeva.vault.toolbox.core.models.StepManifest;
import com.veeva.vault.toolbox.core.models.VpkBuildManifest;
import com.veeva.vault.toolbox.core.results.DeploymentResult;
import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.core.utils.Checksum;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.VaultModel;
import com.veeva.vault.vapil.api.model.common.Job;
import com.veeva.vault.vapil.api.model.response.JobCreateResponse;
import com.veeva.vault.vapil.api.model.response.JobStatusResponse;
import com.veeva.vault.vapil.api.model.response.MdlResponse;
import com.veeva.vault.vapil.api.model.response.PackageDeploymentResultsResponse;
import com.veeva.vault.vapil.api.model.response.PackageImportResultsResponse;
import com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest;
import com.veeva.vault.vapil.api.request.JobRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.veeva.vault.toolbox.core.utils.FileIO.makeDirectories;

/**
 * Represents a Vault Package (VPK) and provides operations to build, package,
 * and deploy it through the Vault Configuration Migration API.
 *
 * <p>Instances are also serialized to and from {@code vaultpackage.xml} via
 * Jackson, so the XML annotations on the fields are part of the public package
 * format.
 */
public class VaultPackage extends VaultModel {

    public static final String VAULTPACKAGE_FILENAME = "vaultpackage.xml";

    private static final Logger logger = LoggerFactory.getLogger(VaultPackage.class);

    private static final int JOB_WAIT_SECONDS = 11;
    private static final int MAX_STATUS_RETRIES = 20;
    private static final String PACKAGE_PATH_TOKEN = "/vault_package__v/";
    private static final String IMPORT_RESULTS_PATH_TOKEN = "/actions/import/results";
    private static final String PACKAGE_STATUS_IMPORTED = "imported__v";
    private static final String PACKAGE_STATUS_VERIFIED = "verified__v";
    private static final String PACKAGE_STATUS_FAILED = "failed__v";
    private static final String PACKAGE_STATUS_DEPLOYED = "deployed__v";
    private static final String PACKAGE_STATUS_WARNINGS = "warnings_encountered__v";

    private static final Set<String> TERMINAL_JOB_STATUSES =
            Set.of("SUCCESS", "FAILED", "ERRORS_ENCOUNTERED", "COMPLETED");
    private static final Set<String> SUPPORTED_VPK_EXTENSIONS = Set.of(
            ".csv", ".dep", ".java", ".json", ".mdl", ".md5",
            ".xml", ".js", ".css", ".png", ".jpg");

    /** XML namespace for the Vault Package manifest. */
    @JacksonXmlProperty(isAttribute = true)
    private String xmlns = "https://veevavault.com/";

    /** The name of the package. */
    @JacksonXmlProperty(localName = "name")
    private String name;

    /** The source information for the package, including Vault ID and author. */
    @JacksonXmlProperty(localName = "source")
    private Source source;

    /** The type of package (e.g., migration, test data). */
    @JacksonXmlProperty(localName = "packagetype")
    private String packageType = PackageType.MIGRATION.getValue();

    /** A brief summary of the package contents. */
    @JacksonXmlProperty(localName = "summary")
    private String summary;

    /** A detailed description of the package. */
    @JacksonXmlProperty(localName = "description")
    private String description;

    /** Java SDK configuration for the package. */
    @JacksonXmlProperty(localName = "javasdk")
    private JavaSdk javaSdk;

    /** The authenticated Vault API client used for deployment operations. */
    private final VaultClient vaultClient;

    /**
     * Creates a new {@code VaultPackage} bound to the given Vault client.
     *
     * @param vaultClient the authenticated Vault API client used for deployment
     *                    operations
     */
    @JsonIgnore
    public VaultPackage(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
    }

    /**
     * Gets the name of the package.
     *
     * @return the package name
     */
    @JsonGetter
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the package.
     *
     * @param name the new package name
     */
    @JsonSetter
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the source of the package.
     *
     * @return the package source
     */
    @JsonGetter
    public Source getSource() {
        return source;
    }

    /**
     * Sets the source of the package.
     *
     * @param source the new package source
     */
    @JsonSetter
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * Gets the package type.
     *
     * @return the package type
     */
    @JsonGetter
    public String getPackageType() {
        return packageType;
    }

    /**
     * Sets the package type as a string.
     *
     * @param packageType the new package type
     */
    @JsonAnySetter
    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    /**
     * Sets the package type.
     *
     * @param packageType the new package type
     */
    @JsonIgnore
    public void setPackageType(PackageType packageType) {
        this.packageType = packageType.getValue();
    }

    /**
     * Gets the summary of the package.
     *
     * @return the package summary
     */
    @JsonGetter
    public String getSummary() {
        return summary;
    }

    /**
     * Sets the summary of the package.
     *
     * @param summary the new summary
     */
    @JsonSetter
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Gets the description of the package.
     *
     * @return the package description
     */
    @JsonGetter
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the package.
     *
     * @param description the new description
     */
    @JsonSetter
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the Java SDK configuration.
     *
     * @return the Java SDK configuration
     */
    @JsonGetter
    public JavaSdk getJavaSdk() {
        return javaSdk;
    }

    /**
     * Sets the Java SDK configuration.
     *
     * @param javaSdk the new Java SDK configuration
     */
    @JsonSetter
    public void setJavaSdk(JavaSdk javaSdk) {
        this.javaSdk = javaSdk;
    }

    /**
     * Writes this package as {@value #VAULTPACKAGE_FILENAME} into
     * {@code outputDir}, creating the directory if necessary.
     *
     * @param outputDir the directory in which to write the manifest
     * @return the manifest file, or {@code null} if writing failed
     */
    @JsonIgnore
    public File createXmlFile(File outputDir) {
        File xmlFile = new File(outputDir, VAULTPACKAGE_FILENAME);
        makeDirectories(outputDir);

        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
            xmlMapper.writeValue(xmlFile, this);
            return xmlFile;
        } catch (Exception e) {
            logger.error("Failed to write " + VAULTPACKAGE_FILENAME, e);
            return null;
        }
    }

    /**
     * Prepares the contents of {@code sourceDirectory} (refreshing checksums
     * and per-component manifests) and zips all supported files into a VPK
     * archive at {@code packageFile}.
     *
     * @param sourceDirectory the directory containing the package contents
     * @param packageFile     the output VPK archive
     */
    @JsonIgnore
    public void pack(File sourceDirectory, File packageFile) {
        try {
            prepVpkFiles(sourceDirectory);
            FileIO.zipFiles(packageFile, getVpkFiles(sourceDirectory), sourceDirectory, null, false);
        } catch (Exception e) {
            logger.error("Failed to pack VPK at " + packageFile, e);
        }
    }

    /**
     * Builds a VPK described by a {@link VpkBuildManifest}: copies Java SDK
     * sources, Web SDK distributions, and component files into a build
     * directory, writes the {@code vaultpackage.xml} manifest, and zips the
     * result into a {@code .vpk} archive.
     *
     * @param buildManifestFile the build manifest file describing the package
     * @param workingDirectory  directory used as the build root and package
     *                          output location
     * @param relativePath      directory used to resolve the relative paths
     *                          referenced by the build manifest
     * @param username          fallback author when the manifest does not
     *                          specify one
     * @param vaultId           Vault ID written to the package source element
     * @return the resulting VPK file
     */
    public File buildFromManifest(File buildManifestFile,
                                  File workingDirectory,
                                  File relativePath,
                                  String username,
                                  Integer vaultId) {
        VpkBuildManifest buildManifest = VpkBuildManifest.load(buildManifestFile);

        File buildDirectory = new File(workingDirectory.getPath(), "build");
        if (buildDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(buildDirectory);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        makeDirectories(buildDirectory);

        setName(buildManifest.getName());
        if (getSource() == null) {
            setSource(new Source());
        }
        getSource().setVault(vaultId);
        getSource().setAuthor(StringUtils.isNotBlank(buildManifest.getAuthor())
                ? buildManifest.getAuthor()
                : username);

        VpkBuildManifest.JavaSdk buildManifestJavaSdk = buildManifest.getJavaSdk();
        if (buildManifestJavaSdk != null) {
            setJavaSdk(new JavaSdk());
            getJavaSdk().setDeploymentOption(buildManifestJavaSdk.getDeploymentOption());

            String localPath = buildManifestJavaSdk.getPath().replace('\\', '/');
            localPath = localPath.substring(localPath.lastIndexOf("src/main/java/com/veeva/vault/custom"));
            File javaSdkBuildDirectory = new File(buildDirectory.getPath(), "javasdk/" + localPath);
            copyFiles(new File(relativePath.getPath(), buildManifestJavaSdk.getPath()), javaSdkBuildDirectory);
        }

        VpkBuildManifest.WebSdk webSdk = buildManifest.getWebSdk();
        if (webSdk != null) {
            if (buildManifestJavaSdk == null) {
                setJavaSdk(new JavaSdk());
                getJavaSdk().setDeploymentOption(JavaSdk.DeploymentOption.INCREMENTAL);
            }

            for (VpkBuildManifest.WebSdk.Distribution distribution : webSdk.getDistributions()) {
                File shell = new File(relativePath.getPath(), distribution.getShell());
                if (shell.exists()) {
                    try {
                        ProcessBuilder pb = new ProcessBuilder("sh", shell.getName());
                        pb.directory(shell.getParentFile());
                        pb.start();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                File distributionDirectory = new File(buildDirectory.getPath(), "websdk/" + distribution.getName());
                makeDirectories(distributionDirectory);

                copyFiles(new File(relativePath.getPath(), distribution.getPath()),
                        new File(distributionDirectory.getPath(), "dist"));
                copyFiles(new File(relativePath.getPath(), distribution.getManifest()),
                        new File(distributionDirectory.getPath(), "distribution-manifest.json"));
            }
        }

        setPackageType(buildManifest.getPackageType());
        setDescription(buildManifest.getDescription());
        setSummary(buildManifest.getSummary());

        List<VpkBuildManifest.Component> components = buildManifest.getComponents();
        if (components != null) {
            for (VpkBuildManifest.Component component : components) {
                File sourceFile = new File(relativePath.getPath(), component.getPath());
                File targetFile = new File(buildDirectory.getPath(),
                        "components/" + component.getStep() + "/" + sourceFile.getName());
                copyFiles(sourceFile, targetFile);

                if (sourceFile.getName().toLowerCase().endsWith(".csv")) {
                    File xmlSourceFile = replaceExtension(sourceFile, ".xml");
                    if (xmlSourceFile.exists()) {
                        copyFiles(xmlSourceFile, new File(targetFile.getParent(), xmlSourceFile.getName()));
                    }
                }
            }
        }

        createXmlFile(buildDirectory);
        File vpkFile = new File(workingDirectory.getPath(), "packages/" + buildManifest.getName() + ".vpk");
        pack(buildDirectory, vpkFile);
        return vpkFile;
    }

    /**
     * Imports {@code packageFile} into Vault, polls the import job until
     * completion, verifies the package, deploys it, and aggregates the
     * deployment outcome into a {@link DeploymentResult}.
     *
     * @param packageFile      the VPK archive to import and deploy
     * @param progressConsumer callback that receives progress updates throughout
     *                         the import, verification, and deployment phases
     * @return a {@link DeploymentResult} describing the outcome and any errors,
     *         warnings, or informational messages produced by Vault
     */
    @JsonIgnore
    public DeploymentResult deployPackage(File packageFile, Consumer<ProgressResult> progressConsumer) {
        DeploymentResult deploymentResult = new DeploymentResult();
        try {
            progressConsumer.accept(new ProgressResult("Preparing Deployment"));

            JobCreateResponse importJobResponse = vaultClient.newRequest(ConfigurationMigrationRequest.class)
                    .setInputPath(packageFile.getAbsolutePath())
                    .importPackage();

            if (importJobResponse.isFailure() || importJobResponse.getJobId() == null) {
                deploymentResult.addErrorMessage("Vault rejected package import: "
                        + describeJobError(importJobResponse));
                return deploymentResult;
            }

            JobStatusResponse importStatusResponse = getJobStatusWithRetry(
                    importJobResponse.getJobId(), progressConsumer, "Import");

            String importStatus = importStatusResponse.getData().getStatus();
            if (!TERMINAL_JOB_STATUSES.contains(importStatus)) {
                return deploymentResult;
            }

            progressConsumer.accept(new ProgressResult("Checking Deployment Status"));

            Job.Link packageLink = importStatusResponse.getData().getLinks().stream()
                    .filter(link -> "artifacts".equals(link.getRel()))
                    .findFirst()
                    .orElse(null);

            if (packageLink == null) {
                deploymentResult.addErrorMessage("Vault job completed with status '" + importStatus
                        + "' but returned no artifact logs. The package may be entirely empty or invalid.");
                return deploymentResult;
            }

            String packageId = extractPackageId(packageLink.getHref());

            PackageImportResultsResponse importResults = vaultClient
                    .newRequest(ConfigurationMigrationRequest.class)
                    .retrievePackageImportResults(packageId);

            if (!importResults.isSuccessful()) {
                deploymentResult.addErrorMessage(importResults.getResponseMessage());
                return deploymentResult;
            }

            progressConsumer.accept(new ProgressResult("Imported Package"));

            String packageStatus = importResults.getVaultPackage().getPackageStatus();
            if (!PACKAGE_STATUS_IMPORTED.equals(packageStatus) && !PACKAGE_STATUS_VERIFIED.equals(packageStatus)) {
                deploymentResult.addErrorMessage("Package verification failed with status: " + packageStatus);
                return deploymentResult;
            }

            progressConsumer.accept(new ProgressResult("Deploying Package"));

            JobCreateResponse deployJobResponse = vaultClient
                    .newRequest(ConfigurationMigrationRequest.class)
                    .deployPackage(packageId);

            if (deployJobResponse.isFailure() || deployJobResponse.getJobId() == null) {
                deploymentResult.addErrorMessage("Vault rejected package deployment: "
                        + describeJobError(deployJobResponse));
                return deploymentResult;
            }

            JobStatusResponse deployStatusResponse = getJobStatusWithRetry(
                    deployJobResponse.getJobId(), progressConsumer, "Deployment");

            if (!TERMINAL_JOB_STATUSES.contains(deployStatusResponse.getData().getStatus())) {
                return deploymentResult;
            }

            PackageDeploymentResultsResponse deployResults = vaultClient
                    .newRequest(ConfigurationMigrationRequest.class)
                    .retrievePackageDeployResults(packageId);

            recordDeploymentOutcome(deployResults, packageFile, progressConsumer, deploymentResult);

        } catch (Exception e) {
            logger.error("Exception during deployment", e);
            deploymentResult.addErrorMessage("Exception during deployment: " + e.getMessage());
        }
        return deploymentResult;
    }

    /**
     * Describes the error contained within a job creation response.
     *
     * @param response the job creation response
     * @return the error message
     */
    private static String describeJobError(JobCreateResponse response) {
        String message = response.getResponseMessage();
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            message += " - " + response.getErrors().get(0).getMessage();
        }
        return message;
    }

    /**
     * Extracts the package ID from the given href string.
     *
     * @param href the href string
     * @return the extracted package ID
     */
    private static String extractPackageId(String href) {
        int start = href.lastIndexOf(PACKAGE_PATH_TOKEN) + PACKAGE_PATH_TOKEN.length();
        int end = href.lastIndexOf(IMPORT_RESULTS_PATH_TOKEN);
        return href.substring(start, end);
    }

    /**
     * Aggregates the results of a package deployment into the {@link DeploymentResult}.
     *
     * @param deployResults    the results returned from the Vault API
     * @param packageFile      the original VPK file
     * @param progressConsumer callback for progress updates
     * @param deploymentResult the result object to populate
     */
    private void recordDeploymentOutcome(PackageDeploymentResultsResponse deployResults,
                                         File packageFile,
                                         Consumer<ProgressResult> progressConsumer,
                                         DeploymentResult deploymentResult) {
        PackageDeploymentResultsResponse.ResponseDetails details = deployResults.getResponseDetails();
        String status = details.getPackageStatus();
        String packageName = packageFile.getName()
                .replaceAll("(?i)\\.vpk$", "")
                .replaceAll("(?i)\\.zip$", "");

        Severity severity = new Severity();
        String statusSuffix = describeStatus(status, severity);

        String friendlyMessage = "Package " + packageName + " has " + statusSuffix + ".";
        progressConsumer.accept(new ProgressResult(friendlyMessage));

        String deployedCount = stringValue(details.get("deployed"), "0");
        String failedCount = stringValue(details.get("failed"), "0");
        String warningsCount = stringValue(details.get("deployed_with_warnings"), "0");

        if (!"0".equals(failedCount)) severity.hasErrors = true;
        if (!"0".equals(warningsCount)) severity.hasWarnings = true;

        String summaryLine = "Summary: " + deployedCount + " Deployed, "
                + failedCount + " Failed, " + warningsCount + " Warnings";

        addBySeverity(deploymentResult, friendlyMessage, severity);
        addBySeverity(deploymentResult, summaryLine, severity);

        recordDeploymentLogs(details, deploymentResult, severity);
        recordPackageStepResults(details, deploymentResult);
    }

    /**
     * Translates a Vault package status into a human-readable description and updates the severity level.
     *
     * @param status   the raw status string from Vault
     * @param severity the severity tracker to update
     * @return a human-readable status description
     */
    private static String describeStatus(String status, Severity severity) {
        if (status == null) {
            return "finished with status: null";
        }
        if (PACKAGE_STATUS_DEPLOYED.equalsIgnoreCase(status)) {
            return "been successfully deployed";
        }
        if (PACKAGE_STATUS_FAILED.equalsIgnoreCase(status)) {
            severity.hasErrors = true;
            return "failed to deploy";
        }
        if (PACKAGE_STATUS_VERIFIED.equalsIgnoreCase(status)) {
            return "been successfully verified";
        }
        if (PACKAGE_STATUS_WARNINGS.equalsIgnoreCase(status)) {
            severity.hasWarnings = true;
            return "been deployed with warnings";
        }
        if ("error".equalsIgnoreCase(status)
                || "error__v".equalsIgnoreCase(status)
                || "errors_encountered__v".equalsIgnoreCase(status)) {
            severity.hasErrors = true;
            return "encountered an error";
        }
        String lower = status.toLowerCase();
        if (lower.contains("error") || lower.contains("fail")) {
            severity.hasErrors = true;
        }
        return "finished with status: " + status.replace("__v", "").replace("_", " ");
    }

    /**
     * Records any deployment log filenames returned by Vault into the deployment result.
     *
     * @param details          the response details containing the log list
     * @param deploymentResult the result object to populate
     * @param severity         the current severity context
     */
    private static void recordDeploymentLogs(PackageDeploymentResultsResponse.ResponseDetails details,
                                             DeploymentResult deploymentResult,
                                             Severity severity) {
        Object rawLogs = details.get("deployment_log");
        if (!(rawLogs instanceof List<?> logs) || logs.isEmpty()) {
            return;
        }

        addBySeverity(deploymentResult, "\nVault Deployment Logs:", severity);
        for (Object rawLog : logs) {
            String filename = readField(rawLog, "filename");
            if (filename != null) {
                addBySeverity(deploymentResult, " • " + filename, severity);
            }
        }
    }

    /**
     * Records detailed results for each package step and component into the deployment result.
     *
     * @param details          the response details containing step and component results
     * @param deploymentResult the result object to populate
     */
    private static void recordPackageStepResults(PackageDeploymentResultsResponse.ResponseDetails details,
                                                 DeploymentResult deploymentResult) {
        Object rawSteps = details.get("package_steps");
        if (!(rawSteps instanceof List<?> steps)) {
            return;
        }

        for (Object rawStep : steps) {
            Object rawComponents = readObject(rawStep, "package_components");
            if (!(rawComponents instanceof List<?> components)) {
                continue;
            }

            for (Object rawComp : components) {
                String compStatus = readField(rawComp, "status");
                String compMessage = readField(rawComp, "response_message");
                String compName = readField(rawComp, "name");
                String compType = readField(rawComp, "type");

                if (compMessage == null || compMessage.isEmpty() || "SUCCESS".equalsIgnoreCase(compMessage)) {
                    continue;
                }

                String detailedMsg = compName + " (" + compType + "): " + compMessage;
                if (PACKAGE_STATUS_FAILED.equalsIgnoreCase(compStatus)) {
                    deploymentResult.addErrorMessage(detailedMsg);
                } else if (PACKAGE_STATUS_WARNINGS.equalsIgnoreCase(compStatus)
                        || compMessage.toLowerCase().contains("warning")) {
                    deploymentResult.addWarnMessage(detailedMsg);
                } else {
                    deploymentResult.addInfoMessage(detailedMsg);
                }
            }
        }
    }

    /**
     * Adds a message to the deployment result based on the specified severity.
     *
     * @param result   the result object to populate
     * @param message  the message to add
     * @param severity the severity context
     */
    private static void addBySeverity(DeploymentResult result, String message, Severity severity) {
        if (severity.hasErrors) {
            result.addErrorMessage(message);
        } else if (severity.hasWarnings) {
            result.addWarnMessage(message);
        } else {
            result.addInfoMessage(message);
        }
    }

    /**
     * Reads a string field from a container object (either a {@link VaultModel} or a {@link Map}).
     *
     * @param container the object to read from
     * @param key       the field name
     * @return the string value, or {@code null} if not found or not a string
     */
    private static String readField(Object container, String key) {
        Object value = readObject(container, key);
        return value instanceof String s ? s : null;
    }

    /**
     * Reads an object from a container (either a {@link VaultModel} or a {@link Map}).
     *
     * @param container the object to read from
     * @param key       the field name
     * @return the object value, or {@code null} if not found
     */
    private static Object readObject(Object container, String key) {
        if (container instanceof VaultModel model) {
            return model.get(key);
        }
        if (container instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    /**
     * Returns the string representation of an object, or a fallback value if the object is null.
     *
     * @param value    the object to convert
     * @param fallback the value to return if {@code value} is {@code null}
     * @return the string value
     */
    private static String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    /**
     * Retrieves the status of a Vault job, retrying if necessary.
     *
     * @param jobId            the ID of the job to poll
     * @param progressConsumer callback for progress updates
     * @param jobType          a label for the job type (e.g., "Import", "Deployment")
     * @return the final {@link JobStatusResponse}, or {@code null} if an error occurred
     */
    private JobStatusResponse getJobStatusWithRetry(int jobId,
                                                    Consumer<ProgressResult> progressConsumer,
                                                    String jobType) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(e.getMessage());
        }

        JobStatusResponse jobStatusResponse = null;
        for (int tries = 0; tries < MAX_STATUS_RETRIES; tries++) {
            if (tries > 0 && !waitWithProgress(jobId, jobType, progressConsumer)) {
                return null;
            }

            progressConsumer.accept(new ProgressResult("Checking job status for Job ID = " + jobId));
            jobStatusResponse = vaultClient.newRequest(JobRequest.class).retrieveJobStatus(jobId);

            if (TERMINAL_JOB_STATUSES.contains(jobStatusResponse.getData().getStatus())) {
                return jobStatusResponse;
            }
        }
        return jobStatusResponse;
    }

    /**
     * Waits for a specified interval while providing progress updates for a specific job type.
     *
     * @param jobId            the ID of the job being waited on
     * @param jobType          the label for the job type
     * @param progressConsumer callback for progress updates
     * @return {@code true} if the wait completed; {@code false} if interrupted
     */
    private static boolean waitWithProgress(int jobId, String jobType, Consumer<ProgressResult> progressConsumer) {
        try {
            for (int i = 0; i < JOB_WAIT_SECONDS; i++) {
                progressConsumer.accept(new ProgressResult("Waiting " + (JOB_WAIT_SECONDS - i)
                        + " seconds for " + jobType + " Job ID = " + jobId));
                Thread.sleep(1000);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Scans the directory for all files matching supported VPK extensions.
     *
     * @param directory the directory to scan
     * @return a list of matching files
     */
    private List<File> getVpkFiles(File directory) {
        return FileIO.getFiles(directory, SUPPORTED_VPK_EXTENSIONS);
    }

    /**
     * Prepares VPK components by generating MD5 checksums and per-component manifests (XML).
     *
     * @param directory the directory containing the components
     */
    private void prepVpkFiles(File directory) {
        List<File> files = getVpkFiles(directory);
        if (files == null) {
            return;
        }

        for (File componentFile : files) {
            try {
                String fileName = componentFile.getName();
                String componentName = fileName.substring(0, fileName.lastIndexOf("."));
                String md5 = Checksum.getMd5(componentFile);
                String lower = fileName.toLowerCase();

                if (lower.endsWith(".mdl")) {
                    prepMdlFile(componentFile, componentName, md5);
                } else if (lower.endsWith(".csv")) {
                    prepCsvFile(componentFile, md5);
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Prepares an MDL component for packaging.
     *
     * @param componentFile the MDL file
     * @param componentName the name of the component
     * @param md5           the MD5 checksum of the file
     * @throws IOException if file access fails
     */
    private void prepMdlFile(File componentFile, String componentName, String md5) throws IOException {
        String content = new String(Files.readAllBytes(componentFile.toPath()), StandardCharsets.UTF_8);
        if (isMultiMdl(content)) {
            prepMultiMdlManifest(componentFile, componentName, md5);
        } else {
            prepSingleMdlChecksum(componentFile, componentName, md5);
        }
    }

    /**
     * Prepares a manifest (XML) for a multi-command MDL file.
     *
     * @param componentFile the MDL file
     * @param componentName the name of the component
     * @param md5           the MD5 checksum of the file
     * @throws IOException if file access fails
     */
    private void prepMultiMdlManifest(File componentFile, String componentName, String md5) throws IOException {
        File md5File = replaceExtension(componentFile, ".md5");
        if (md5File.exists()) {
            md5File.delete();
        }

        File xmlFile = replaceExtension(componentFile, ".xml");
        logger.info(xmlFile.getAbsolutePath());

        StepManifest stepManifest;
        if (xmlFile.exists()) {
            String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
            stepManifest = new XmlMapper().readValue(xmlContent, StepManifest.class);
        } else {
            stepManifest = new StepManifest();
            stepManifest.setLabel(componentName);
        }

        if (!md5.equals(stepManifest.getChecksum())) {
            stepManifest.setChecksum(md5);
            logger.info("RECREATE: " + xmlFile);
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
            xmlMapper.writeValue(xmlFile, stepManifest);
        } else {
            logger.info("VALID: " + xmlFile);
        }
    }

    /**
     * Prepares a simple checksum file (.md5) for a single-command MDL file.
     *
     * @param componentFile the MDL file
     * @param componentName the name of the component
     * @param md5           the MD5 checksum of the file
     * @throws IOException if file access fails
     */
    private void prepSingleMdlChecksum(File componentFile, String componentName, String md5) throws IOException {
        File md5File = new File(componentFile.getParent(), componentName + ".md5");
        String validContent = md5 + " " + componentName;

        if (!md5File.exists()) {
            logger.info("CREATE: " + md5File);
            FileIO.writeFileContent(md5File, validContent);
            return;
        }

        String existingContent = new String(Files.readAllBytes(md5File.toPath()), StandardCharsets.UTF_8);
        if (!validContent.equals(existingContent)) {
            logger.info("RECREATE: " + md5File);
            FileIO.writeFileContent(md5File, validContent);
        } else {
            logger.info("VALID: " + md5File);
        }
    }

    /**
     * Prepares a manifest (XML) for a CSV data component.
     *
     * @param componentFile the CSV file
     * @param md5           the MD5 checksum of the file
     * @throws IOException if file access fails
     */
    private void prepCsvFile(File componentFile, String md5) throws IOException {
        File xmlFile = replaceExtension(componentFile, ".xml");
        CsvManifest csvManifest;
        if (xmlFile.exists()) {
            String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
            csvManifest = new XmlMapper().readValue(xmlContent, CsvManifest.class);
        } else {
            csvManifest = new CsvManifest();
            csvManifest.setLabel(componentFile.getName());
            csvManifest.setCsvDataStep(new CsvDataStep());
        }

        boolean hasChange = false;
        if (!md5.equals(csvManifest.getChecksum())) {
            csvManifest.setChecksum(md5);
            hasChange = true;
        }

        CsvDataStep csvDataStep = csvManifest.getCsvDataStep();
        if (csvDataStep != null) {
            int rowCount = FileIO.getCsvRowCount(componentFile) - 1;
            if (csvDataStep.getRecordCount() == null || csvDataStep.getRecordCount() != rowCount) {
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

    /**
     * Replaces the file extension of the given file with a new one.
     *
     * @param file         the original file
     * @param newExtension the new extension (including the dot)
     * @return a new File object with the updated extension
     */
    private static File replaceExtension(File file, String newExtension) {
        String path = file.getAbsolutePath();
        return new File(path.substring(0, path.lastIndexOf(".")) + newExtension);
    }

    /**
     * Heuristically determines if an MDL file contains multiple commands.
     *
     * @param mdl the MDL content to analyze
     * @return {@code true} if multiple commands are detected; {@code false} otherwise
     */
    private static boolean isMultiMdl(String mdl) {
        if (mdl == null) {
            return false;
        }
        String testMdl = ";" + StringUtils.normalizeSpace(mdl).replace(" ", "");
        int commandCount = 0;
        for (MdlResponse.CommandType commandType : MdlResponse.CommandType.values()) {
            commandCount += StringUtils.countMatches(testMdl, ";" + commandType.getValue());
        }
        return commandCount > 1;
    }

    /**
     * Recursively copies files or directories from source to target.
     *
     * @param source the source file or directory
     * @param target the target file or directory
     */
    private void copyFiles(File source, File target) {
        if (source.isDirectory()) {
            makeDirectories(target);
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyFiles(file, new File(target, file.getName()));
                }
            }
            return;
        }

        try {
            makeDirectories(target.getParentFile());
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /** Tracker for the highest severity level encountered during deployment. */
    private static final class Severity {
        boolean hasErrors;
        boolean hasWarnings;
    }

    /** Vault package types accepted by the Configuration Migration API. */
    public enum PackageType {
        MIGRATION("migration__v"),
        TESTDATA("test_data__sys");

        private final String value;

        /**
         * Constructs the PackageType.
         *
         * @param value the string value
         */
        PackageType(String value) {
            this.value = value;
        }

        /**
         * Gets the string value of the package type.
         *
         * @return the string value
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * Java SDK section of the package manifest. Carries the deployment option
     * that controls how the Java SDK contents are applied during deployment.
     */
    public static class JavaSdk extends VaultModel {

        /** Strategies for applying Java SDK contents during deployment. */
        public enum DeploymentOption {
            NONE("none"),
            DELETE_ALL("delete_all"),
            INCREMENTAL("incremental"),
            REPLACE_ALL("replace_all");

            private final String value;

            /**
             * Constructs the DeploymentOption.
             *
             * @param value the string value
             */
            DeploymentOption(String value) {
                this.value = value;
            }

            /**
             * Gets the string value of the deployment option.
             *
             * @return the string value
             */
            public String getValue() {
                return value;
            }
        }

        @JacksonXmlProperty(localName = "deployment_option")
        private String deploymentOption;

        /**
         * Constructs a new JavaSdk.
         */
        @JsonIgnore
        public JavaSdk() {
        }

        /**
         * Constructs a new JavaSdk with the given deployment option.
         *
         * @param deploymentOption the deployment option
         */
        @JsonIgnore
        public JavaSdk(DeploymentOption deploymentOption) {
            setDeploymentOption(deploymentOption);
        }

        /**
         * Gets the deployment option.
         *
         * @return the deployment option
         */
        @JsonGetter
        public String getDeploymentOption() {
            return deploymentOption;
        }

        /**
         * Sets the deployment option.
         *
         * @param deploymentOption the new deployment option
         * @return this JavaSdk instance
         */
        @JsonAnySetter
        public JavaSdk setDeploymentOption(String deploymentOption) {
            this.deploymentOption = deploymentOption;
            return this;
        }

        /**
         * Sets the deployment option using the enum.
         *
         * @param deploymentOption the new deployment option
         */
        @JsonIgnore
        public void setDeploymentOption(DeploymentOption deploymentOption) {
            this.deploymentOption = deploymentOption.getValue();
        }
    }

    /** Source section of the package manifest, identifying its origin Vault. */
    public static class Source extends VaultModel {

        @JacksonXmlProperty(localName = "vault")
        private Integer vault;

        @JacksonXmlProperty(localName = "author")
        private String author;

        /**
         * Gets the vault ID.
         *
         * @return the vault ID
         */
        @JsonGetter
        public Integer getVault() {
            return vault;
        }

        /**
         * Sets the vault ID.
         *
         * @param vault the new vault ID
         */
        @JsonSetter
        public void setVault(Integer vault) {
            this.vault = vault;
        }

        /**
         * Gets the author.
         *
         * @return the author
         */
        @JsonGetter
        public String getAuthor() {
            return author;
        }

        /**
         * Sets the author.
         *
         * @param author the new author
         */
        @JsonSetter
        public void setAuthor(String author) {
            this.author = author;
        }
    }
}
