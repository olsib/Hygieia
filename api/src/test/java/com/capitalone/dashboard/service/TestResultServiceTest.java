package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.TestCase;
import com.capitalone.dashboard.model.TestCaseStatus;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.model.TestSuite;
import com.capitalone.dashboard.model.TestSuiteType;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.CustodianResultRepository;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import com.capitalone.dashboard.request.CustodianResultRequest;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustodianResultServiceTest {

    @Mock private CustodianResultRepository CustodianResultRepository;
    @Mock private CollectorRepository collectorRepository;
    @Mock private CollectorService collectorService;
    @Mock private ComponentRepository componentRepository;
    @InjectMocks private CustodianResultServiceImpl CustodianResultService;


    @Test
    public void createWithGoodRequest() throws HygieiaException {
        ObjectId collectorId = ObjectId.get();

        TestDataCreateRequest request = makeTestDateCreateRequest();

        when(collectorRepository.findOne(collectorId)).thenReturn(new Collector());
        when(collectorService.createCollector(any(Collector.class))).thenReturn(new Collector());
        when(collectorService.createCollectorItem(any(CollectorItem.class))).thenReturn(new CollectorItem());

        CustodianResult CustodianResult = makeCustodianResult();

        when(CustodianResultRepository.save(any(CustodianResult.class))).thenReturn(CustodianResult);
        String response = CustodianResultService.create(request);
        String expected = CustodianResult.getId().toString();
        assertEquals(response, expected);
    }

    @Test
    public void createV2WithGoodRequest() throws HygieiaException {
        ObjectId collectorId = ObjectId.get();

        TestDataCreateRequest request = makeTestDateCreateRequest();

        when(collectorRepository.findOne(collectorId)).thenReturn(new Collector());
        when(collectorService.createCollector(any(Collector.class))).thenReturn(new Collector());
        when(collectorService.createCollectorItem(any(CollectorItem.class))).thenReturn(new CollectorItem());

        CustodianResult CustodianResult = makeCustodianResult();

        when(CustodianResultRepository.save(any(CustodianResult.class))).thenReturn(CustodianResult);
        String response = CustodianResultService.createV2(request);
        String expected = CustodianResult.getId().toString() + "," + CustodianResult.getCollectorItemId();
        assertEquals(response, expected);
    }

    @Test
    public void search_Empty_Response_No_CollectorItems() {
        ObjectId collectorItemId = ObjectId.get();
        ObjectId collectorId = ObjectId.get();

        CustodianResultRequest request = new CustodianResultRequest();

        when(componentRepository.findOne(request.getComponentId())).thenReturn(makeComponent(collectorItemId, collectorId, false));
        when(collectorRepository.findOne(collectorId)).thenReturn(new Collector());

        DataResponse<Iterable<CustodianResult>> response = CustodianResultService.search(request);

        List<CustodianResult> result = (List<CustodianResult>) response.getResult();
        Assert.assertNull(result);
    }

    @Test
    public void search_Empty_Response_No_Component() {
        ObjectId collectorId = ObjectId.get();
        CustodianResultRequest request = new CustodianResultRequest();

        when(componentRepository.findOne(request.getComponentId())).thenReturn(null);
        when(collectorRepository.findOne(collectorId)).thenReturn(new Collector());

        DataResponse<Iterable<CustodianResult>> response = CustodianResultService.search(request);

        List<CustodianResult> result = (List<CustodianResult>) response.getResult();
        Assert.assertNull(result);
    }

    private Component makeComponent(ObjectId collectorItemId, ObjectId collectorId, boolean populateCollectorItems) {
        CollectorItem item = new CollectorItem();
        item.setId(collectorItemId);
        item.setCollectorId(collectorId);
        Component c = new Component();
        if (populateCollectorItems) {
            c.getCollectorItems().put(CollectorType.Test, Collections.singletonList(item));
        }
        return c;
    }

    private TestDataCreateRequest makeTestDateCreateRequest() {
        TestDataCreateRequest data = new TestDataCreateRequest();
        data.setExecutionId(ObjectId.get().toString());
        data.setTestJobId(ObjectId.get().toString());
        data.setDescription("description");
        data.setDuration(1L);
        data.setExecutionId("execution ID");
        data.setStartTime(2L);
        data.setEndTime(3L);
        data.setFailureCount(1);
        data.setSuccessCount(2);
        data.setSkippedCount(0);
        data.setTotalCount(3);

        TestCapability capability = new TestCapability();
        capability.setDescription("description");
        capability.setDuration(1L);
        capability.setStartTime(2L);
        capability.setEndTime(3L);
        capability.setFailedTestSuiteCount(1);
        capability.setSkippedTestSuiteCount(2);
        capability.setSuccessTestSuiteCount(3);
        capability.setTotalTestSuiteCount(6);

        TestSuite suite = new TestSuite();
        suite.setDescription("description");
        suite.setDuration(1L);
        suite.setStartTime(2L);
        suite.setEndTime(3L);
        suite.setType(TestSuiteType.Functional);
        suite.setFailedTestCaseCount(1);
        suite.setSuccessTestCaseCount(2);
        suite.setSkippedTestCaseCount(0);
        suite.setTotalTestCaseCount(3);

        capability.getTestSuites().add(suite);
        data.getTestCapabilities().add(capability);

        TestCase testCase = new TestCase();
        testCase.setId("id");
        testCase.setDescription("description");
        testCase.setStatus(TestCaseStatus.Failure);
        testCase.setDuration(20l);

        suite.getTestCases().add(testCase);

        return data;
    }


    private CustodianResult makeCustodianResult() {
        CustodianResult result = new CustodianResult();
        result.setId(ObjectId.get());
        result.setCollectorItemId(ObjectId.get());
        result.setDescription("description");
        result.setDuration(1L);
        result.setExecutionId("execution ID");
        result.setStartTime(2L);
        result.setEndTime(3L);
        result.setUrl("http://foo.com");
        result.setFailureCount(1);
        result.setSuccessCount(2);
        result.setSkippedCount(0);
        result.setTotalCount(3);

        TestCapability capability = new TestCapability();
        capability.setDescription("description");
        capability.setDuration(1l);
        capability.setStartTime(2l);
        capability.setEndTime(3l);
        capability.setFailedTestSuiteCount(1);
        capability.setSkippedTestSuiteCount(2);
        capability.setSuccessTestSuiteCount(3);
        capability.setTotalTestSuiteCount(6);

        TestSuite suite = new TestSuite();
        suite.setDescription("description");
        suite.setDuration(1L);
        suite.setStartTime(2L);
        suite.setEndTime(3L);
        suite.setType(TestSuiteType.Functional);
        suite.setFailedTestCaseCount(1);
        suite.setSuccessTestCaseCount(2);
        suite.setSkippedTestCaseCount(0);
        suite.setTotalTestCaseCount(3);

        capability.getTestSuites().add(suite);
        result.getTestCapabilities().add(capability);

        TestCase testCase = new TestCase();
        testCase.setId("id");
        testCase.setDescription("description");
        testCase.setStatus(TestCaseStatus.Failure);
        testCase.setDuration(20L);

        suite.getTestCases().add(testCase);

        return result;
    }


}
