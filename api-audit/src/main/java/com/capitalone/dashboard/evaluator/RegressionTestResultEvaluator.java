package com.capitalone.dashboard.evaluator;

import com.capitalone.dashboard.ApiSettings;
import com.capitalone.dashboard.model.AuditException;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.DashboardType;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.model.TestSuiteType;
import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.TestSuite;
import com.capitalone.dashboard.model.Traceability;
import com.capitalone.dashboard.model.Widget;
import com.capitalone.dashboard.model.Feature;
import com.capitalone.dashboard.model.StoryIndicator;

import com.capitalone.dashboard.repository.FeatureRepository;
import com.capitalone.dashboard.repository.CustodianResultRepository;
import com.capitalone.dashboard.response.CustodianResultsAuditResponse;
import com.capitalone.dashboard.status.CustodianResultAuditStatus;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Comparator;
import java.util.Optional;
import java.util.Map;

import java.util.regex.Pattern;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

@Component
public class RegressionCustodianResultEvaluator extends Evaluator<CustodianResultsAuditResponse> {

    private final CustodianResultRepository CustodianResultRepository;
    private final FeatureRepository featureRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(RegressionCustodianResultEvaluator.class);
    private long beginDate;
    private long endDate;
    private Dashboard dashboard;
    private static final String WIDGET_CODE_ANALYSIS = "codeanalysis";
    private static final String WIDGET_FEATURE = "feature";
    private static final String STR_TEAM_ID = "teamId";
    private static final String STR_UNDERSCORE = "_";
    private static final String STR_HYPHEN = "-";
    private static final String STR_AT = "@";
    private static final String STR_EMPTY = "";
    private static final String SUCCESS_COUNT = "successCount";
    private static final String FAILURE_COUNT = "failureCount";
    private static final String SKIP_COUNT = "skippedCount";
    private static final String TOTAL_COUNT = "totalCount";
    private static final String PRIORITY_HIGH = "High";

    @Autowired
    public RegressionCustodianResultEvaluator(CustodianResultRepository CustodianResultRepository, FeatureRepository featureRepository) {
        this.CustodianResultRepository = CustodianResultRepository;
        this.featureRepository = featureRepository;
    }

    @Override
    public Collection<CustodianResultsAuditResponse> evaluate(Dashboard dashboard, long beginDate, long endDate, Map<?, ?> dummy) throws AuditException {
        this.beginDate = beginDate-1;
        this.endDate = endDate+1;
        this.dashboard = getDashboard(dashboard.getTitle(), DashboardType.Team);
        List<CollectorItem> testItems = getCollectorItems(this.dashboard, WIDGET_CODE_ANALYSIS, CollectorType.Test);
        Collection<CustodianResultsAuditResponse> CustodianResultsAuditResponse = new ArrayList<>();
        if (CollectionUtils.isEmpty(testItems)) {
            throw new AuditException("No tests configured", AuditException.NO_COLLECTOR_ITEM_CONFIGURED);
        }
        testItems.forEach(testItem -> CustodianResultsAuditResponse.add(getRegressionCustodianResultAudit(dashboard, testItem)));
        return CustodianResultsAuditResponse;
    }

    @Override
    public CustodianResultsAuditResponse evaluate(CollectorItem collectorItem, long beginDate, long endDate, Map<?, ?> data) {
        return new CustodianResultsAuditResponse();
    }

    /**
     * Gets the json response from test_results collection with story information based on tags.
     * @param testItem
     * @return
     */
    protected CustodianResultsAuditResponse getRegressionCustodianResultAudit(Dashboard dashboard, CollectorItem testItem) {
        List<CustodianResult> CustodianResults = CustodianResultRepository.findByCollectorItemIdAndTimestampIsBetweenOrderByTimestampDesc(testItem.getId(), beginDate, endDate);
        return performCustodianResultAudit(dashboard, testItem, CustodianResults);
    }

    /**
     * Perform test result audit
     *
     * @param testItem
     * @param CustodianResults
     * @return CustodianResultsAuditResponse
     */
    private CustodianResultsAuditResponse performCustodianResultAudit(Dashboard dashboard, CollectorItem testItem, List<CustodianResult> CustodianResults) {

        CustodianResultsAuditResponse CustodianResultsAuditResponse = new CustodianResultsAuditResponse();
        CustodianResultsAuditResponse.setAuditEntity(testItem.getOptions());
        CustodianResultsAuditResponse.setLastUpdated(testItem.getLastUpdated());
        if (CollectionUtils.isEmpty(CustodianResults) || !isValidCustodianResultTestSuitType(CustodianResults)){
            CustodianResultsAuditResponse.addAuditStatus(CustodianResultAuditStatus.TEST_RESULT_MISSING);
            return CustodianResultsAuditResponse;
        }
        CustodianResult CustodianResult = CustodianResults.stream().sorted(Comparator.comparing(CustodianResult::getTimestamp).reversed()).findFirst().get();

        CustodianResultsAuditResponse.setLastExecutionTime(CustodianResult.getStartTime());
        CustodianResultsAuditResponse.setType(CustodianResult.getType().toString());
        CustodianResultsAuditResponse.setFeatureCustodianResult(getFeatureCustodianResult(CustodianResult));
        CustodianResultsAuditResponse = updateTraceabilityDetails(dashboard, CustodianResult, CustodianResultsAuditResponse);

        List<TestCapability> testCapabilities = CustodianResult.getTestCapabilities().stream().collect(Collectors.toList());
        CustodianResultsAuditResponse = updateCustodianResultAuditStatuses(testCapabilities, CustodianResultsAuditResponse);

        // Clearing for readability in response
        for(TestCapability test: testCapabilities){
            test.setTestSuites(null);
        }
        CustodianResultsAuditResponse.setTestCapabilities(testCapabilities);
        return CustodianResultsAuditResponse;

    }

    /***
     * Update traceability details with calculated percent value
     * @param CustodianResult,CustodianResultsAuditResponse
     * @return CustodianResultsAuditResponse
     */
    private CustodianResultsAuditResponse updateTraceabilityDetails(Dashboard dashboard, CustodianResult CustodianResult, CustodianResultsAuditResponse CustodianResultsAuditResponse) {

        Traceability traceability = new Traceability();
        List<String> totalStoriesList = new ArrayList<>();
        List<String> totalCompletedStories = new ArrayList<>();
        List<HashMap> totalStories = new ArrayList<>();
        double traceabilityThreshold = settings.getTraceabilityThreshold();

        Widget featureWidget = getFeatureWidget(dashboard);
        Optional<Object> teamIdOpt = Optional.ofNullable(featureWidget.getOptions().get(STR_TEAM_ID));
        String teamId = teamIdOpt.isPresent() ? teamIdOpt.get().toString() : "";
        List<Feature> featureList = featureRepository.getStoryByTeamID(teamId);

        featureList.stream().forEach(feature -> {
            HashMap<String, String> storyAuditStatusMap = new HashMap<>();
            totalStoriesList.add(feature.getsNumber());

            if(isValidChangeDate(feature)) {
                if(this.isValidStoryStatus(feature.getsStatus())){
                    totalCompletedStories.add(feature.getsNumber());
                    storyAuditStatusMap.put(feature.getsNumber(), CustodianResultAuditStatus.TEST_RESULTS_TRACEABILITY_STORY_MATCH.name());
                } else{
                    storyAuditStatusMap.put(feature.getsNumber(), CustodianResultAuditStatus.TEST_RESULTS_TRACEABILITY_STORY_STATUS_INVALID.name());
                }
            } else {
                storyAuditStatusMap.put(feature.getsNumber(), CustodianResultAuditStatus.TEST_RESULTS_TRACEABILITY_STORY_NOT_FOUND.name());
            }
            totalStories.add(storyAuditStatusMap);
        });
        if (totalCompletedStories.size() > NumberUtils.INTEGER_ZERO) {
            int totalStoryIndicatorCount = getTotalStoryIndicators(CustodianResult).size();
            double percentage = (totalStoryIndicatorCount * 100) / totalCompletedStories.size();
            traceability.setPercentage(percentage);

            if (traceabilityThreshold == NumberUtils.DOUBLE_ZERO) {
                CustodianResultsAuditResponse.addAuditStatus(CustodianResultAuditStatus.TEST_RESULTS_TRACEABILITY_THRESHOLD_DEFAULT);
            }
            if(percentage == NumberUtils.DOUBLE_ZERO){
                CustodianResultsAuditResponse.addAuditStatus(CustodianResultAuditStatus.TEST_RESULTS_TRACEABILITY_NOT_FOUND);
            }
        } else {
            CustodianResultsAuditResponse.addAuditStatus(CustodianResultAuditStatus.TEST_RESULTS_TRACEABILITY_NOT_FOUND_IN_GIVEN_DATE_RANGE);
        }
        traceability.setTotalCompletedStories(totalCompletedStories);
        traceability.setTotalStories(totalStories);
        traceability.setTotalStoryCount(totalStories.size());
        traceability.setThreshold(traceabilityThreshold);
        CustodianResultsAuditResponse.setTraceability(traceability);
        return CustodianResultsAuditResponse;
    }

    /**
     * Get story indicators by matching test case tags with feature stories
     * @param CustodianResult
     * @return
     */
    private  List<StoryIndicator> getTotalStoryIndicators(CustodianResult CustodianResult) {

        Pattern featureIdPattern = Pattern.compile(settings.getFeatureIDPattern());
        List<StoryIndicator> totalStoryIndicatorList = new ArrayList<>();
        CustodianResult.getTestCapabilities().stream()
                .map(TestCapability::getTestSuites).flatMap(Collection::stream)
                .map(TestSuite::getTestCases).flatMap(Collection::stream)
                .forEach(testCase -> {
                    List<StoryIndicator> storyIndicatorList = new ArrayList<>();
                    testCase.getTags().forEach(tag -> {
                        if (featureIdPattern.matcher(getValidFeatureId(tag)).find()) {
                            List<Feature> features = featureRepository.getStoryByNumber(tag);
                            features.forEach(feature -> {
                                if (isValidChangeDate(feature) && isValidStoryStatus(feature.getsStatus())) {
                                    StoryIndicator storyIndicator = new StoryIndicator();
                                    storyIndicator.setStoryId(feature.getsId());
                                    storyIndicator.setStoryType(feature.getsTypeName());
                                    storyIndicator.setStoryNumber(feature.getsNumber());
                                    storyIndicator.setStoryName(feature.getsName());
                                    storyIndicator.setEpicNumber(feature.getsEpicNumber());
                                    storyIndicator.setEpicName(feature.getsEpicName());
                                    storyIndicator.setProjectName(feature.getsProjectName());
                                    storyIndicator.setTeamName(feature.getsTeamName());
                                    storyIndicator.setSprintName(feature.getsSprintName());
                                    storyIndicator.setStoryStatus(feature.getsStatus());
                                    storyIndicator.setStoryState(feature.getsState());
                                    storyIndicatorList.add(storyIndicator);
                                }
                            });
                    }
                    });
                    storyIndicatorList.forEach(storyIndicator -> {
                        if (!totalStoryIndicatorList.contains(storyIndicator)) {
                            totalStoryIndicatorList.add(storyIndicator);
                        }
                    });
                    testCase.setStoryIndicators(storyIndicatorList);
                });
        return totalStoryIndicatorList;
    }

    private CharSequence getValidFeatureId(String tag) {
        tag = tag.replaceAll(STR_UNDERSCORE, STR_HYPHEN).replaceAll(STR_AT, STR_EMPTY);
        return tag;
    }

    /**
     * Coverts the Human readable time date to Epoch Time Stamp in Milliseconds
     * @param feature
     * @return
     */
    private long getEpochChangeDate(Feature feature) {
        String datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        long changeDate = 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
            Date dt = sdf.parse(feature.getChangeDate());
            changeDate = dt.getTime();
        } catch(ParseException e) {
            e.printStackTrace();
            LOGGER.error("Error in RegressionCustodianResultEvaluator.getEpochChangeDate() - Unable to match date pattern - " + e.getMessage());
        }

        return changeDate;
    }

    /**
     * Check whether the story status is valid
     * @param storyStatus
     * @return
     */
    private boolean isValidStoryStatus(String storyStatus) {
        final List<String> validStatus = settings.getValidStoryStatus();
        return validStatus.contains(storyStatus.toUpperCase());
    }

    /**
     * Check whether the feature date is valid
     * @param feature
     * @return
     */
    private boolean isValidChangeDate(Feature feature){
        return (this.getEpochChangeDate(feature) >= beginDate && this.getEpochChangeDate(feature) <= endDate);
    }

    /**
     * Get dashboard by title and type
     * @param title
     * @param dashboardType
     * @return
     */
    private Dashboard getDashboard(String title, DashboardType dashboardType) {
        return dashboardRepository.findByTitleAndType(title, dashboardType);
    }

    /**
     * Check whether the test result test suit type is valid
     * @param CustodianResults
     * @return
     */
    public boolean isValidCustodianResultTestSuitType(List<CustodianResult> CustodianResults) {
        return CustodianResults.stream()
                .anyMatch(CustodianResult -> testResult.getType().equals(TestSuiteType.Functional)
                        || testResult.getType().equals(TestSuiteType.Manual)
                        || testResult.getType().equals(TestSuiteType.Regression));
    }

    /**
     * Get feature widget
     * @return
     */
    public Widget getFeatureWidget(Dashboard dashboard) {
        return dashboard.getWidgets()
                .stream()
                .filter(widget -> widget.getName().equalsIgnoreCase(WIDGET_FEATURE))
                .findFirst().orElse(new Widget());
    }
    /**
     * Builds feature test result data map
     * @param testResult
     * @return featureTestResultMap
     */
    protected HashMap getFeatureTestResult(TestResult testResult) {
        HashMap<String,Integer> featureTestResultMap = new HashMap<>();
        featureTestResultMap.put(SUCCESS_COUNT, testResult.getSuccessCount());
        featureTestResultMap.put(FAILURE_COUNT, testResult.getFailureCount());
        featureTestResultMap.put(SKIP_COUNT, testResult.getSkippedCount());
        featureTestResultMap.put(TOTAL_COUNT,testResult.getTotalCount());
        return featureTestResultMap;
    }

    /**
     * update test result audit statuses
     * @param testCapabilities
     * @param testResultsAuditResponse
     * @return
     */
    private TestResultsAuditResponse updateTestResultAuditStatuses(List<TestCapability> testCapabilities, TestResultsAuditResponse testResultsAuditResponse) {

        boolean isSuccessHighPriority = settings.getTestResultSuccessPriority().equalsIgnoreCase(PRIORITY_HIGH);
        boolean isFailureHighPriority = settings.getTestResultFailurePriority().equalsIgnoreCase(PRIORITY_HIGH);

        if(isAllTestCasesSkipped(testCapabilities)){
            testResultsAuditResponse.addAuditStatus(TestResultAuditStatus.TEST_RESULT_SKIPPED);
            return testResultsAuditResponse;
        }
        double testCasePassPercent = this.getTestCasePassPercent(testCapabilities);
        if (isFailureHighPriority){
            if (testCasePassPercent < settings.getTestResultThreshold()) {
                testResultsAuditResponse.addAuditStatus(TestResultAuditStatus.TEST_RESULT_AUDIT_FAIL);
            } else {
                testResultsAuditResponse.addAuditStatus(TestResultAuditStatus.TEST_RESULT_AUDIT_OK);
            }
        }else if (isSuccessHighPriority){
            if (testCasePassPercent > NumberUtils.INTEGER_ZERO) {
                testResultsAuditResponse.addAuditStatus(TestResultAuditStatus.TEST_RESULT_AUDIT_OK);
            } else {
                testResultsAuditResponse.addAuditStatus(TestResultAuditStatus.TEST_RESULT_AUDIT_FAIL);
            }
        }else {
            testResultsAuditResponse.addAuditStatus(TestResultAuditStatus.TEST_RESULT_MISSING);
        }
        return testResultsAuditResponse;
    }

    /**
     * Get test result pass percent
     * @param testCapabilities
     * @return
     */
    private double getTestCasePassPercent(List<TestCapability> testCapabilities) {
        double testCaseSuccessCount = testCapabilities.stream().mapToDouble(testCapability ->
             testCapability.getTestSuites().parallelStream().mapToDouble(TestSuite::getSuccessTestCaseCount).sum()
        ).sum();
        double totalTestCaseCount = testCapabilities.stream().mapToDouble(testCapability ->
                testCapability.getTestSuites().parallelStream().mapToDouble(TestSuite::getTotalTestCaseCount).sum()
        ).sum();

        return (testCaseSuccessCount/totalTestCaseCount) * 100;
    }

    public void setSettings(ApiSettings settings) {
        this.settings = settings;
    }

    /**
     * Check if all the test cases are skipped
     * @param testCapabilities
     * @return
     */
    public boolean isAllTestCasesSkipped(List<TestCapability> testCapabilities) {
        int totalTestCaseCount = testCapabilities.stream().mapToInt(testCapability ->
                testCapability.getTestSuites().parallelStream().mapToInt(TestSuite::getTotalTestCaseCount).sum()
        ).sum();
        int testCaseSkippedCount = testCapabilities.stream().mapToInt(testCapability ->
                testCapability.getTestSuites().parallelStream().mapToInt(TestSuite::getSkippedTestCaseCount).sum()
        ).sum();

        boolean isSkippedHighPriority = settings.getTestResultSkippedPriority().equalsIgnoreCase(PRIORITY_HIGH);

        if ((testCaseSkippedCount >= totalTestCaseCount) && isSkippedHighPriority){
            return true;
        }
        return false;
    }
}