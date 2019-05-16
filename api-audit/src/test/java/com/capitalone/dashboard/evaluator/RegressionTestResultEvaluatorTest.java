package com.capitalone.dashboard.evaluator;

import com.capitalone.dashboard.ApiSettings;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.DashboardType;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.model.TestSuiteType;
import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.TestSuite;
import com.capitalone.dashboard.model.Widget;
import com.capitalone.dashboard.model.Feature;

import com.capitalone.dashboard.repository.FeatureRepository;
import com.capitalone.dashboard.repository.CustodianResultRepository;
import com.capitalone.dashboard.response.CustodianResultsAuditResponse;
import com.capitalone.dashboard.status.CustodianResultAuditStatus;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RegressionCustodianResultEvaluatorTest {

    @InjectMocks
    private RegressionCustodianResultEvaluator regressionCustodianResultEvaluator;

    @Mock
    private CustodianResultRepository CustodianResultRepository;

    @Mock
    private FeatureRepository featureRepository;

    @Before
    public void setup(){
        regressionCustodianResultEvaluator.setSettings(getSettings());
    }

    @Test
    public void evaluate_CustodianResultMissing(){
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(ObjectId.get());

        List<CustodianResult> emptyCustodianResults = new ArrayList<>();
        when(CustodianResultRepository.findByCollectorItemIdAndTimestampIsBetweenOrderByTimestampDesc(collectorItem.getId(),
                123456789, 123456989)).thenReturn(emptyCustodianResults);
        CustodianResultsAuditResponse CustodianResultsAuditResponse = regressionCustodianResultEvaluator.getRegressionCustodianResultAudit(getDashboard(), collectorItem);
        Assert.assertTrue(CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_MISSING));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_OK));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_FAIL));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_SKIPPED));
    }

    @Test
    public void evaluate_CustodianResultAuditOK(){
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(ObjectId.get());
        List<CustodianResult> CustodianResults = Arrays.asList(getAuditOKCustodianResult());
        when(CustodianResultRepository.findByCollectorItemIdAndTimestampIsBetweenOrderByTimestampDesc(any(ObjectId.class),
                any(Long.class), any(Long.class))).thenReturn(CustodianResults);
        when(featureRepository.getStoryByTeamID("TEST-1234")).thenReturn(Arrays.asList(new Feature()));
        CustodianResultsAuditResponse CustodianResultsAuditResponse = regressionCustodianResultEvaluator.getRegressionCustodianResultAudit(getDashboard(), collectorItem);
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_MISSING));
        Assert.assertTrue(CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_OK));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_FAIL));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_SKIPPED));
    }

    @Test
    public void evaluate_CustodianResultAuditFAIL(){
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(ObjectId.get());
        List<CustodianResult> CustodianResults = Arrays.asList(getAuditFAILCustodianResult());
        when(CustodianResultRepository.findByCollectorItemIdAndTimestampIsBetweenOrderByTimestampDesc(any(ObjectId.class),
                any(Long.class), any(Long.class))).thenReturn(CustodianResults);
        when(featureRepository.getStoryByTeamID("TEST-1234")).thenReturn(Arrays.asList(new Feature()));
        CustodianResultsAuditResponse CustodianResultsAuditResponse = regressionCustodianResultEvaluator.getRegressionCustodianResultAudit(getDashboard(), collectorItem);
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_MISSING));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_OK));
        Assert.assertTrue(CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_FAIL));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_SKIPPED));
    }

    @Test
    public void evaluate_CustodianResultAuditSKIP(){
        CollectorItem collectorItem = new CollectorItem();
        collectorItem.setId(ObjectId.get());
        List<CustodianResult> CustodianResults = Arrays.asList(getAuditSKIPCustodianResult());
        when(CustodianResultRepository.findByCollectorItemIdAndTimestampIsBetweenOrderByTimestampDesc(any(ObjectId.class),
                any(Long.class), any(Long.class))).thenReturn(CustodianResults);
        when(featureRepository.getStoryByTeamID("TEST-1234")).thenReturn(Arrays.asList(new Feature()));
        CustodianResultsAuditResponse CustodianResultsAuditResponse = regressionCustodianResultEvaluator.getRegressionCustodianResultAudit(getDashboard(), collectorItem);
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_MISSING));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_OK));
        Assert.assertTrue(!CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_AUDIT_FAIL));
        Assert.assertTrue(CustodianResultsAuditResponse.getAuditStatuses().contains(CustodianResultAuditStatus.TEST_RESULT_SKIPPED));
    }

    @Test
    public void evaluate_featureCustodianResult() {
        CustodianResult CustodianResult = getCustodianResult();
        HashMap featureTestMap = regressionCustodianResultEvaluator.getFeatureCustodianResult(CustodianResult);
        Assert.assertEquals(CustodianResult.getSuccessCount(), Integer.parseInt(featureTestMap.get("successCount").toString()));
        Assert.assertEquals(CustodianResult.getFailureCount(), Integer.parseInt(featureTestMap.get("failureCount").toString()));
        Assert.assertEquals(CustodianResult.getSkippedCount(), Integer.parseInt(featureTestMap.get("skippedCount").toString()));
        Assert.assertEquals(CustodianResult.getTotalCount(), Integer.parseInt(featureTestMap.get("totalCount").toString()));
    }

    @Test
    public void evaluate_traceability_featureWidgetConfig() {
        Dashboard dashboard = getDashboard();
        Widget widget1 = new Widget();
        widget1.setName("TestWidget");
        dashboard.getWidgets().add(widget1);
        Widget emptyWidget = regressionCustodianResultEvaluator.getFeatureWidget(dashboard);
        Assert.assertNotEquals(emptyWidget.getName(), "feature");
        Widget widget2 = new Widget();
        widget2.setName("feature");
        dashboard.getWidgets().add(widget2);
        Widget featureWidget = regressionCustodianResultEvaluator.getFeatureWidget(dashboard);
        Assert.assertEquals(featureWidget.getName(), "feature");
    }

    private CustodianResult getCustodianResult() {
        CustodianResult CustodianResult = new CustodianResult();
        CustodianResult.setType(TestSuiteType.Functional);
        CustodianResult.setSuccessCount(10);
        CustodianResult.setFailureCount(5);
        CustodianResult.setSkippedCount(1);
        CustodianResult.setTotalCount(16);
        return CustodianResult;
    }
    private CustodianResult getAuditOKCustodianResult() {
        CustodianResult CustodianResult = new CustodianResult();
        CustodianResult.setType(TestSuiteType.Regression);
        TestCapability testCapability = new TestCapability();

        TestSuite testSuite1 = new TestSuite();
        testSuite1.setSuccessTestCaseCount(18);
        testSuite1.setFailedTestCaseCount(1);
        testSuite1.setSkippedTestCaseCount(1);
        testSuite1.setTotalTestCaseCount(20);

        TestSuite testSuite2 = new TestSuite();
        testSuite2.setSuccessTestCaseCount(20);
        testSuite2.setFailedTestCaseCount(0);
        testSuite2.setSkippedTestCaseCount(0);
        testSuite2.setTotalTestCaseCount(20);

        testCapability.getTestSuites().add(testSuite1);
        testCapability.getTestSuites().add(testSuite2);
        CustodianResult.getTestCapabilities().add(testCapability);
        return CustodianResult;
    }

    private CustodianResult getAuditFAILCustodianResult() {
        CustodianResult CustodianResult = new CustodianResult();
        CustodianResult.setType(TestSuiteType.Regression);
        TestCapability testCapability = new TestCapability();


        TestSuite testSuite = new TestSuite();
        testSuite.setSuccessTestCaseCount(37);
        testSuite.setFailedTestCaseCount(2);
        testSuite.setSkippedTestCaseCount(1);
        testSuite.setTotalTestCaseCount(40);
        testCapability.getTestSuites().add(testSuite);
        CustodianResult.getTestCapabilities().add(testCapability);
        return CustodianResult;
    }

    public CustodianResult getAuditSKIPTestResult() {
        TestResult testResult = new TestResult();
        testResult.setType(TestSuiteType.Functional);
        TestCapability testCapability = new TestCapability();
        TestSuite testSuite = new TestSuite();
        testSuite.setSuccessTestCaseCount(0);
        testSuite.setFailedTestCaseCount(0);
        testSuite.setSkippedTestCaseCount(40);
        testSuite.setTotalTestCaseCount(40);
        testCapability.getTestSuites().add(testSuite);
        testResult.getTestCapabilities().add(testCapability);
        return testResult;
    }

    public Dashboard getDashboard() {
        Dashboard dashboard = new Dashboard("Template1", "Title1", null, null, DashboardType.Team,
                "ASV1", "BAP1", null, false, null);
        return dashboard;
    }

    private ApiSettings getSettings(){
        ApiSettings settings = new ApiSettings();
        settings.setTestResultSuccessPriority("Low");
        settings.setTestResultFailurePriority("High");
        settings.setTestResultSkippedPriority("High");
        settings.setTestResultThreshold(95.0);
        return settings;
    }
}