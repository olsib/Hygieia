package com.capitalone.dashboard.evaluator;

import com.capitalone.dashboard.model.AuditException;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.PerfIndicators;
import com.capitalone.dashboard.model.PerfTest;
import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.TestCase;
import com.capitalone.dashboard.model.TestCaseStep;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.model.TestSuite;
import com.capitalone.dashboard.model.TestSuiteType;
import com.capitalone.dashboard.repository.CustodianResultRepository;
import com.capitalone.dashboard.response.PerformanceTestAuditResponse;
import com.capitalone.dashboard.response.CustodianResultsAuditResponse;
import com.capitalone.dashboard.status.PerformanceTestAuditStatus;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PerformanceCustodianResultEvaluator extends Evaluator<PerformanceTestAuditResponse> {

    private final CustodianResultRepository CustodianResultRepository;


    @Autowired
    public PerformanceCustodianResultEvaluator(CustodianResultRepository CustodianResultRepository) {

        this.CustodianResultRepository = CustodianResultRepository;
    }

    @Override
    public Collection<PerformanceTestAuditResponse> evaluate(Dashboard dashboard, long beginDate, long endDate, Map<?, ?> dummy) throws AuditException {
        List<CollectorItem> testItems = getCollectorItems(dashboard, "codeanalysis", CollectorType.Test);
        Collection<CustodianResultsAuditResponse> responses = new ArrayList<>();
        if (CollectionUtils.isEmpty(testItems)) {
            throw new AuditException("No tests configured", AuditException.NO_COLLECTOR_ITEM_CONFIGURED);
        }

        return testItems.stream().map(item -> evaluate(item, beginDate, endDate, null)).collect(Collectors.toList());
    }

    @Override
    public PerformanceTestAuditResponse evaluate(CollectorItem collectorItem, long beginDate, long endDate, Map<?, ?> dummy) {
        return getPerformanceTestAudit(collectorItem, beginDate, endDate);
    }

    private PerformanceTestAuditResponse getPerformanceTestAudit(CollectorItem perfItem, long beginDate, long endDate) {

        PerformanceTestAuditResponse perfReviewResponse = new PerformanceTestAuditResponse();
        if (perfItem == null) {
            perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.COLLECTOR_ITEM_ERROR);
            return perfReviewResponse;
        }
        List<CustodianResult> CustodianResults = CustodianResultRepository.findByCollectorItemIdAndTimestampIsBetweenOrderByTimestampDesc(perfItem.getId(), beginDate-1, endDate+1);
        List<PerfTest> testlist = new ArrayList<>();

        if (!CollectionUtils.isEmpty(CustodianResults)){
            CustodianResults.sort(Comparator.comparing(CustodianResult::getTimestamp).reversed());
            CustodianResult CustodianResult = CustodianResults.iterator().next();
            if (TestSuiteType.Performance.toString().equalsIgnoreCase(CustodianResult.getType().name())) {
                Collection<TestCapability> testCapabilities = CustodianResult.getTestCapabilities();

                if(!CollectionUtils.isEmpty(testCapabilities)){
                    Comparator<TestCapability> testCapabilityComparator = Comparator.comparing(TestCapability::getTimestamp);
                    List<TestCapability> tc = new ArrayList<>(testCapabilities);
                    Collections.sort(tc,testCapabilityComparator.reversed());
                    TestCapability testCapability =  tc.get(0);
                    PerfTest test = new PerfTest();
                    List<PerfIndicators> kpilist = new ArrayList<>();
                    Collection<TestSuite> testSuites = testCapability.getTestSuites();
                    for (TestSuite testSuite : testSuites) {
                        Collection<TestCase> testCases = testSuite.getTestCases();
                        for (TestCase testCase : testCases) {
                            PerfIndicators kpi = new PerfIndicators();
                            kpi.setStatus(testCase.getStatus().toString());
                            kpi.setType(testCase.getDescription().toString());
                            Collection<TestCaseStep> testSteps = testCase.getTestSteps();
                            int j = 0;
                            for (TestCaseStep testCaseStep : testSteps) {
                                String value = testCaseStep.getDescription();
                                if (j == 0) {
                                    kpi.setTarget(Double.parseDouble(value));
                                    if(StringUtils.equalsIgnoreCase(testCase.getDescription(),"KPI : Avg response times") && !value.isEmpty()){
                                        perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_THRESHOLDS_RESPONSE_TIME_FOUND);
                                    }else if((StringUtils.equalsIgnoreCase(testCase.getDescription(),"KPI : Transaction Per Second") && !value.isEmpty())) {
                                        perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_THRESHOLDS_TRANSACTIONS_PER_SECOND_FOUND);
                                    }else if(StringUtils.equalsIgnoreCase(testCase.getDescription(),"KPI : Error Rate Threshold") && !value.isEmpty() ){
                                        perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_THRESHOLDS_ERROR_RATE_FOUND);
                                    }
                                }
                                if (j == 1) kpi.setAchieved(Double.parseDouble(value));
                                j++;
                            }
                            kpilist.add(kpi);
                            if(StringUtils.equalsIgnoreCase(kpi.getType(),"KPI : Avg response times")&& (kpi.getTarget() > kpi.getAchieved()))
                            {
                                perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_THRESHOLD_RESPONSE_TIME_MET);
                            }else if(StringUtils.equalsIgnoreCase(kpi.getType(),"KPI : Transaction Per Second")&& (kpi.getTarget() <= kpi.getAchieved())){
                                perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_THRESHOLD_TRANSACTIONS_PER_SECOND_MET);
                            }else if(StringUtils.equalsIgnoreCase(kpi.getType(),"KPI : Error Rate Threshold")&& (kpi.getTarget() >= kpi.getAchieved())){
                                perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_THRESHOLD_ERROR_RATE_MET);
                            }

                        }
                        if(StringUtils.equalsIgnoreCase(CustodianResult.getDescription(),"Success")){
                            perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_MET);

                        }
                        test.setRunId(CustodianResult.getExecutionId());
                        test.setStartTime(CustodianResult.getStartTime());
                        test.setEndTime(CustodianResult.getEndTime());
                        test.setResultStatus(CustodianResult.getResultStatus());
                        test.setPerfIndicators(kpilist);
                        test.setTestName(testSuite.getDescription());
                        test.setTimeStamp(CustodianResult.getTimestamp());
                        testlist.add(test);
                    }
                }

                testlist.sort(Comparator.comparing(PerfTest::getStartTime).reversed());
                perfReviewResponse.setResult(testlist);
                perfReviewResponse.setLastExecutionTime(CollectionUtils.isEmpty(testlist)?0L:testlist.get(0).getStartTime());
                perfReviewResponse.addAuditStatus((int) testlist.stream().filter(list -> Optional.ofNullable(list).isPresent() && Optional.ofNullable(list.getResultStatus()).isPresent() && list.getResultStatus().matches("Success")).count() > 0 ?
                        PerformanceTestAuditStatus.PERF_RESULT_AUDIT_OK : PerformanceTestAuditStatus.PERF_RESULT_AUDIT_FAIL);
            }
        }
        if (CollectionUtils.isEmpty(testlist)) {
            perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERF_RESULT_AUDIT_MISSING);
        }else {
            perfReviewResponse.addAuditStatus(PerformanceTestAuditStatus.PERFORMANCE_COMMIT_IS_CURRENT);
        }
        return perfReviewResponse;
    }
}
