package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.QCustodianResult;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.CustodianResultRepository;
import com.capitalone.dashboard.request.CollectorRequest;
import com.capitalone.dashboard.request.PerfTestDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import com.capitalone.dashboard.request.CustodianResultRequest;
import com.google.common.collect.Lists;
import com.mysema.query.BooleanBuilder;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustodianResultServiceImpl implements CustodianResultService {

    private final CustodianResultRepository CustodianResultRepository;
    private final ComponentRepository componentRepository;
    private final CollectorRepository collectorRepository;
    private final CollectorService collectorService;

    @Autowired
    public CustodianResultServiceImpl(CustodianResultRepository CustodianResultRepository,
                                 ComponentRepository componentRepository,
                                 CollectorRepository collectorRepository,
                                 CollectorService collectorService) {
        this.CustodianResultRepository = CustodianResultRepository;
        this.componentRepository = componentRepository;
        this.collectorRepository = collectorRepository;
        this.collectorService = collectorService;
    }

    @Override
    public DataResponse<Iterable<CustodianResult>> search(CustodianResultRequest request) {
        Component component = componentRepository.findOne(request.getComponentId());

        if ((component == null) || !component.getCollectorItems().containsKey(CollectorType.Test)) {
            return new DataResponse<>(null, 0L);
        }
        List<CustodianResult> result = new ArrayList<>();
        validateAllCollectorItems(request, component, result);
        //One collector per Type. get(0) is hardcoded.
        if (!CollectionUtils.isEmpty(component.getCollectorItems().get(CollectorType.Test)) && (component.getCollectorItems().get(CollectorType.Test).get(0) != null)) {
            Collector collector = collectorRepository.findOne(component.getCollectorItems().get(CollectorType.Test).get(0).getCollectorId());
            if (collector != null) {
                return new DataResponse<>(pruneToDepth(result, request.getDepth()), collector.getLastExecuted());
            }
        }

        return new DataResponse<>(null, 0L);
    }



    private void validateAllCollectorItems(CustodianResultRequest request, Component component, List<CustodianResult> result) {
        // add all test result repos
        component.getCollectorItems().get(CollectorType.Test).forEach(item -> {
            QCustodianResult CustodianResult = new QCustodianResult("CustodianResult");
            BooleanBuilder builder = new BooleanBuilder();
            builder.and(CustodianResult.collectorItemId.eq(item.getId()));
            validateStartDateRange(request, CustodianResult, builder);
            validateEndDateRange(request, CustodianResult, builder);
            validateDurationRange(request, CustodianResult, builder);
            validateTestCapabilities(request, CustodianResult, builder);
            addAllCustodianResultRepositories(request, result, CustodianResult, builder);
        });
    }

    private void addAllCustodianResultRepositories(CustodianResultRequest request, List<CustodianResult> result, QCustodianResult CustodianResult, BooleanBuilder builder) {
        if (request.getMax() == null) {
            result.addAll(Lists.newArrayList(CustodianResultRepository.findAll(builder.getValue(), CustodianResult.timestamp.desc())));
        } else {
            PageRequest pageRequest = new PageRequest(0, request.getMax(), Sort.Direction.DESC, "timestamp");
            result.addAll(Lists.newArrayList(CustodianResultRepository.findAll(builder.getValue(), pageRequest).getContent()));
        }
    }

    private void validateTestCapabilities(CustodianResultRequest request, QCustodianResult CustodianResult, BooleanBuilder builder) {
        if (!request.getTypes().isEmpty()) {
            builder.and(CustodianResult.testCapabilities.any().type.in(request.getTypes()));
        }
    }

    private void validateDurationRange(CustodianResultRequest request, QCustodianResult CustodianResult, BooleanBuilder builder) {
        if (request.validDurationRange()) {
            builder.and(CustodianResult.duration.between(request.getDurationGreaterThan(), request.getDurationLessThan()));
        }
    }

    private void validateEndDateRange(CustodianResultRequest request, QCustodianResult CustodianResult, BooleanBuilder builder) {
        if (request.validEndDateRange()) {
            builder.and(CustodianResult.endTime.between(request.getEndDateBegins(), request.getEndDateEnds()));
        }
    }

    private void validateStartDateRange(CustodianResultRequest request, QCustodianResult CustodianResult, BooleanBuilder builder) {
        if (request.validStartDateRange()) {
            builder.and(CustodianResult.startTime.between(request.getStartDateBegins(), request.getStartDateEnds()));
        }
    }

    private Iterable<CustodianResult> pruneToDepth(List<CustodianResult> results, Integer depth) {
        // Prune the response to the requested depth
        // 0 - CustodianResult
        // 1 - TestCapability
        // 2 - TestSuite
        // 3 - TestCase
        // 4 - Entire response
        // null - Entire response
        if (depth == null || depth > 3) {
            return results;
        }
        results.forEach(result -> {
            if (depth == 0) {
                result.getTestCapabilities().clear();
            } else {
                result.getTestCapabilities().forEach(testCapability -> {
                    if (depth == 1) {
                        testCapability.getTestSuites().clear();
                    } else {
                        testCapability.getTestSuites().forEach(testSuite -> {
                            if (depth == 2) {
                                testSuite.getTestCases().clear();
                            } else { // depth == 3
                                testSuite.getTestCases().forEach(testCase -> {
                                    testCase.getTestSteps().clear();
                                });
                            }
                        });
                    }
                });
            }
        });

        return results;
    }


    protected CustodianResult createTest(TestDataCreateRequest request) throws HygieiaException {
        /*
          Step 1: create Collector if not there
          Step 2: create Collector item if not there
          Step 3: Insert test data if new. If existing, update it
         */
        Collector collector = createCollector();

        if (collector == null) {
            throw new HygieiaException("Failed creating Test collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }

        CollectorItem collectorItem = createCollectorItem(collector, request);

        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Test collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }


        CustodianResult CustodianResult = createTest(collectorItem, request);


        if (CustodianResult == null) {
            throw new HygieiaException("Failed inserting/updating Test information.", HygieiaException.ERROR_INSERTING_DATA);
        }

        return CustodianResult;

    }

    @Override
    public String create(TestDataCreateRequest request) throws HygieiaException {
        CustodianResult CustodianResult = createTest(request);
        return CustodianResult.getId().toString();
    }

    @Override
    public String createV2(TestDataCreateRequest request) throws HygieiaException {
        CustodianResult CustodianResult = createTest(request);
        return CustodianResult.getId().toString() + "," + CustodianResult.getCollectorItemId().toString();
    }



    protected CustodianResult createPerfTest(PerfTestDataCreateRequest request) throws HygieiaException {
        /**
         * Step 1: create performance Collector if not there
         * Step 2: create Perfomance Collector item if not there
         * Step 3: Insert performance test data if new. If existing, update it
         */
        Collector collector = createGenericCollector(request.getPerfTool());
        if (collector == null) {
            throw new HygieiaException("Failed creating Test collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }
        CollectorItem collectorItem = createGenericCollectorItem(collector, request);
        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Test collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }
        CustodianResult CustodianResult = createPerfTest(collectorItem, request);
        if (CustodianResult == null) {
            throw new HygieiaException("Failed inserting/updating Test information.", HygieiaException.ERROR_INSERTING_DATA);
        }
        return CustodianResult;

    }

    @Override
    public String createPerf(PerfTestDataCreateRequest request) throws HygieiaException {
        CustodianResult CustodianResult = createPerfTest(request);
        return CustodianResult.getId().toString();
    }

    @Override
    public String createPerfV2(PerfTestDataCreateRequest request) throws HygieiaException {
        CustodianResult CustodianResult = createPerfTest(request);
        return CustodianResult.getId().toString() + "," + CustodianResult.getCollectorItemId().toString();
    }

    private Collector createCollector() {
        CollectorRequest collectorReq = new CollectorRequest();
        collectorReq.setName("JenkinsCucumberTest");
        collectorReq.setCollectorType(CollectorType.Test);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());
        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put("jobUrl", "");
        allOptions.put("instanceUrl", "");
        allOptions.put("jobName","");
        col.setAllFields(allOptions);
        //Combination of jobName and jobUrl should be unique always.
        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put("jobUrl", "");
        uniqueOptions.put("jobName","");
        col.setUniqueFields(uniqueOptions);
        return collectorService.createCollector(col);
    }


    private Collector createGenericCollector(String performanceTool) {
        CollectorRequest collectorReq = new CollectorRequest();
        collectorReq.setName(performanceTool);
        collectorReq.setCollectorType(CollectorType.Test);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());
        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put("jobName","");
        allOptions.put("instanceUrl", "");
        col.setAllFields(allOptions);
        col.setUniqueFields(allOptions);
        return collectorService.createCollector(col);
    }

    private CollectorItem createCollectorItem(Collector collector, TestDataCreateRequest request) throws HygieiaException {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(request.getDescription());
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        Map<String, Object> option = new HashMap<>();
        option.put("jobName", request.getTestJobName());
        option.put("jobUrl", request.getTestJobUrl());
        option.put("instanceUrl", request.getServerUrl());
        tempCi.getOptions().putAll(option);
        tempCi.setNiceName(request.getNiceName());
        if (StringUtils.isEmpty(tempCi.getNiceName())) {
            return collectorService.createCollectorItem(tempCi);
        }
        return collectorService.createCollectorItemByNiceNameAndJobName(tempCi, request.getTestJobName());
    }


    private CollectorItem createGenericCollectorItem(Collector collector, PerfTestDataCreateRequest request) {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(request.getPerfTool()+" : "+request.getTestName());
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        Map<String, Object> option = new HashMap<>();
        option.put("jobName", request.getTestName());
        option.put("instanceUrl", request.getInstanceUrl());
        tempCi.getOptions().putAll(option);
        tempCi.setNiceName(request.getPerfTool());
        return collectorService.createCollectorItem(tempCi);
    }


    private CustodianResult createTest(CollectorItem collectorItem, TestDataCreateRequest request) {
        CustodianResult CustodianResult = CustodianResultRepository.findByCollectorItemIdAndExecutionId(collectorItem.getId(),
                request.getExecutionId());
        if (CustodianResult == null) {
            CustodianResult = new CustodianResult();
        }

        CustodianResult.setTargetAppName(request.getTargetAppName());
        CustodianResult.setTargetEnvName(request.getTargetEnvName());
        CustodianResult.setCollectorItemId(collectorItem.getId());
        CustodianResult.setType(request.getType());
        CustodianResult.setDescription(request.getDescription());
        CustodianResult.setDuration(request.getDuration());
        CustodianResult.setEndTime(request.getEndTime());
        CustodianResult.setExecutionId(request.getExecutionId());
        CustodianResult.setFailureCount(request.getFailureCount());
        CustodianResult.setSkippedCount(request.getSkippedCount());
        CustodianResult.setStartTime(request.getStartTime());
        CustodianResult.setSuccessCount(request.getSuccessCount());
        if(request.getTimestamp() == 0) request.setTimestamp(System.currentTimeMillis());
        CustodianResult.setTimestamp(request.getTimestamp());
        CustodianResult.setTotalCount(request.getTotalCount());
        CustodianResult.setUnknownStatusCount(request.getUnknownStatusCount());
        CustodianResult.setUrl(request.getTestJobUrl());
        CustodianResult.getTestCapabilities().addAll(request.getTestCapabilities());
        CustodianResult.setBuildId(new ObjectId(request.getTestJobId()));

        return CustodianResultRepository.save(CustodianResult);
    }

    private CustodianResult createPerfTest(CollectorItem collectorItem, PerfTestDataCreateRequest request) {
        CustodianResult CustodianResult = CustodianResultRepository.findByCollectorItemIdAndExecutionId(collectorItem.getId(),
                request.getRunId());
        if (CustodianResult == null) {
            CustodianResult = new CustodianResult();
        }
        CustodianResult.setTargetAppName(request.getTargetAppName());
        CustodianResult.setTargetEnvName(request.getTargetEnvName());
        CustodianResult.setCollectorItemId(collectorItem.getId());
        CustodianResult.setType(request.getType());
        CustodianResult.setDescription(request.getDescription());
        CustodianResult.setDuration(request.getDuration());
        CustodianResult.setEndTime(request.getEndTime());
        CustodianResult.setExecutionId(request.getRunId());
        CustodianResult.setFailureCount(request.getFailureCount());
        CustodianResult.setSkippedCount(request.getSkippedCount());
        CustodianResult.setStartTime(request.getStartTime());
        CustodianResult.setSuccessCount(request.getSuccessCount());
        if(request.getTimestamp() == 0) request.setTimestamp(System.currentTimeMillis());
        CustodianResult.setTimestamp(request.getTimestamp());
        CustodianResult.setTotalCount(request.getTotalCount());
        CustodianResult.setUnknownStatusCount(request.getUnknownStatusCount());
        CustodianResult.setUrl(request.getReportUrl());
        CustodianResult.getTestCapabilities().addAll(request.getTestCapabilities());
        CustodianResult.setDescription(request.getDescription());
        CustodianResult.setResultStatus(request.getResultStatus());
        return CustodianResultRepository.save(CustodianResult);
    }

}
