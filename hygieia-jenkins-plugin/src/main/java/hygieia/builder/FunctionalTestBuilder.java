package hygieia.builder;

import com.capitalone.dashboard.model.BuildStatus;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.model.quality.QualityVisitee;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hygieia.transformer.CustodianResultVisitor;
import hygieia.utils.HygieiaUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;


public class FunctionalTestBuilder {
    private static final Logger logger = Logger.getLogger(FunctionalTestBuilder.class.getName());
    private ObjectMapper objectMapper;

    public FunctionalTestBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private CustodianResult buildCustodianResults(Run run, TaskListener listener, String filePattern, FilePath filePath, String directory, BuildDataCreateRequest buildDataCreateRequest, String testType) {
        List<FilePath> testFiles = null;
        try {
            EnvVars envVars = run.getEnvironment(listener);
            FilePath rootDirectory = filePath.withSuffix(directory);
            if (envVars != null) {
                filePattern = envVars.expand(filePattern);
            }
            testFiles = Lists.newArrayList(HygieiaUtils.getArtifactFiles(rootDirectory, filePattern, new ArrayList<FilePath>()));
            listener.getLogger().println("Hygieia Test Result Publisher - Looking for file pattern '" + filePattern + "' in directory " + rootDirectory.getRemote());
        } catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("Hygieia Test Result Publisher" + Arrays.toString(e.getStackTrace()));
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("Hygieia Test Result Publisher - InterruptedException on " + Arrays.toString(e.getStackTrace()));
        }
        return getCapabilities(testFiles, listener, String.valueOf(buildDataCreateRequest.getNumber()), buildDataCreateRequest, testType);
    }

    private CustodianResult getCapabilities(List<FilePath> testFiles, TaskListener listener, String executionId, BuildDataCreateRequest buildDataCreateRequest, String testType) {

        CustodianResultVisitor cucumberTransformer = new CustodianResultVisitor(testType, buildDataCreateRequest);
        for (FilePath file : testFiles) {
            try {
                listener.getLogger().println("Hygieia Test Publisher: Processing file: " + file.getRemote());
                QualityVisitee report = objectMapper.readValue(file.readToString(), QualityVisitee.class);
                cucumberTransformer.setCurrentDescriprion(getCapabilityDescription(file));
                report.accept(cucumberTransformer);
            } catch (IOException e) {
                listener.getLogger().println("Hygieia Test Publisher: Processing read error: " + file.getRemote());
            } catch (InterruptedException e) {
                listener.getLogger().println("Hygieia Test Publisher: Processing interrupted: " + file.getRemote());
            }
        }
        return cucumberTransformer.produceResult();
    }

    private static String getCapabilityDescription(FilePath file) {
        String newFileName = file.getRemote().replace(file.getName(), "");
        boolean isUnix = newFileName.endsWith("/");
        int lastFolderIndex;
        newFileName = newFileName.substring(0, newFileName.length() - 1);
        if (isUnix) {
            lastFolderIndex = newFileName.lastIndexOf("/");
        } else {
            lastFolderIndex = newFileName.lastIndexOf("\\");
        }
        if (lastFolderIndex > 0) {
            return newFileName.substring(lastFolderIndex);
        }
        return newFileName;
    }


    public TestDataCreateRequest getTestDataCreateRequest(Run run, TaskListener listener, BuildStatus buildStatus, FilePath filePath, String applicationName, String environmentName, String testType, String filePattern, String directory, String jenkinsName, String buildId) {

        BuildDataCreateRequest buildDataCreateRequest = new BuildBuilder()
                .createBuildRequestFromRun(run, jenkinsName, listener, buildStatus, false);

        CustodianResult CustodianResult = buildCustodianResults(run, listener, filePattern, filePath, directory, buildDataCreateRequest, testType);

        if (CustodianResult != null) {
            TestDataCreateRequest request = new TestDataCreateRequest();
            EnvVars env = null;
            try {
                env = run.getEnvironment(listener);
            } catch (IOException | InterruptedException e) {
                logger.warning("Error getting environment variables");
            }
            if (env != null) {
                request.setServerUrl(env.get("JENKINS_URL"));
            } else {
                String jobPath = "/job" + "/" + buildDataCreateRequest.getJobName() + "/";
                int ind = buildDataCreateRequest.getJobUrl().indexOf(jobPath);
                request.setServerUrl(buildDataCreateRequest.getJobUrl().substring(0, ind));
            }
            request.setTestJobId(buildId);
            request.setType(CustodianResult.getType());
            request.setTestJobName(buildDataCreateRequest.getJobName());
            request.setTestJobUrl(buildDataCreateRequest.getJobUrl());
            request.setTimestamp(CustodianResult.getTimestamp());
            request.setNiceName(jenkinsName);

            request.setDescription(CustodianResult.getDescription());
            request.setDuration(CustodianResult.getDuration());
            request.setEndTime(CustodianResult.getEndTime());
            request.setExecutionId(CustodianResult.getExecutionId());
            request.setFailureCount(CustodianResult.getFailureCount());
            request.setSkippedCount(CustodianResult.getSkippedCount());
            request.setStartTime(CustodianResult.getStartTime());
            request.setSuccessCount(CustodianResult.getSuccessCount());

            request.setTotalCount(CustodianResult.getTotalCount());
            request.setUnknownStatusCount(CustodianResult.getUnknownStatusCount());
            request.getTestCapabilities().addAll(CustodianResult.getTestCapabilities());

            request.setTargetAppName(applicationName);
            request.setTargetEnvName(environmentName);
            return request;
        }
        return null;
    }
}