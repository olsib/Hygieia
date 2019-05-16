package com.capitalone.dashboard.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.capitalone.dashboard.config.TestConfig;
import com.capitalone.dashboard.config.WebMVCConfig;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.TestCase;
import com.capitalone.dashboard.model.TestCaseStatus;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.model.TestSuite;
import com.capitalone.dashboard.model.TestSuiteType;
import com.capitalone.dashboard.request.CustodianResultRequest;
import com.capitalone.dashboard.service.CustodianResultService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class, WebMVCConfig.class})
@WebAppConfiguration
public class CustodianResultControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;
    @Autowired private CustodianResultService CustodianResultService;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testSuites() throws Exception {
        CustodianResult CustodianResult = makeCustodianResult();
        Iterable<CustodianResult> results = Arrays.asList(CustodianResult);
        DataResponse<Iterable<CustodianResult>> response = new DataResponse<>(results, 1);
        TestCapability testCapability = CustodianResult.getTestCapabilities().iterator().next();
        TestSuite testSuite = testCapability.getTestSuites().iterator().next();
        TestCase testCase = testSuite.getTestCases().iterator().next();

        when(CustodianResultService.search(Mockito.any(CustodianResultRequest.class))).thenReturn(response);

        mockMvc.perform(get("/quality/test?componentId=" + ObjectId.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].id", is(CustodianResult.getId().toString())))
                .andExpect(jsonPath("$.result[0].collectorItemId", is(CustodianResult.getCollectorItemId().toString())))
                .andExpect(jsonPath("$.result[0].timestamp", is(intVal(CustodianResult.getTimestamp()))))
                .andExpect(jsonPath("$.result[0].executionId", is(CustodianResult.getExecutionId())))
                .andExpect(jsonPath("$.result[0].description", is(CustodianResult.getDescription())))
                .andExpect(jsonPath("$.result[0].url", is(CustodianResult.getUrl())))
                .andExpect(jsonPath("$.result[0].startTime", is(intVal(CustodianResult.getStartTime()))))
                .andExpect(jsonPath("$.result[0].endTime", is(intVal(CustodianResult.getEndTime()))))
                .andExpect(jsonPath("$.result[0].duration", is(intVal(CustodianResult.getDuration()))))
                .andExpect(jsonPath("$.result[0].failureCount", is(intVal(CustodianResult.getFailureCount()))))
                .andExpect(jsonPath("$.result[0].successCount", is(intVal(CustodianResult.getSuccessCount()))))
                .andExpect(jsonPath("$.result[0].skippedCount", is(intVal(CustodianResult.getSkippedCount()))))
                .andExpect(jsonPath("$.result[0].totalCount", is(intVal(CustodianResult.getTotalCount()))))
                .andExpect(jsonPath("$.result[0].testCapabilities", hasSize(1)))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].description", is(testCapability.getDescription())))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].startTime", is(intVal(testCapability.getStartTime()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].endTime", is(intVal(testCapability.getEndTime()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].duration", is(intVal(testCapability.getDuration()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites", hasSize(1)))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].description", is(testSuite.getDescription())))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].type", is(testSuite.getType().toString())))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].startTime", is(intVal(CustodianResult.getStartTime()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].endTime", is(intVal(CustodianResult.getEndTime()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].duration", is(intVal(CustodianResult.getDuration()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].failedTestCaseCount", is(intVal(CustodianResult.getFailureCount()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].successTestCaseCount", is(intVal(CustodianResult.getSuccessCount()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].skippedTestCaseCount", is(intVal(CustodianResult.getSkippedCount()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].totalTestCaseCount", is(intVal(CustodianResult.getTotalCount()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].unknownStatusCount", is(intVal(CustodianResult.getUnknownStatusCount()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].testCases", hasSize(1)))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].testCases[0].id", is(testCase.getId())))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].testCases[0].description", is(testCase.getDescription())))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].testCases[0].duration", is(intVal(testCase.getDuration()))))
                .andExpect(jsonPath("$.result[0].testCapabilities[0].testSuites[0].testCases[0].status", is(testCase.getStatus().toString())));
    }

    private CustodianResult makeCustodianResult() {
        CustodianResult result = new CustodianResult();
        result.setId(ObjectId.get());
        result.setCollectorItemId(ObjectId.get());
        result.setDescription("description");
        result.setDuration(1l);
        result.setExecutionId("execution ID");
        result.setStartTime(2l);
        result.setEndTime(3l);
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
        suite.setDuration(1l);
        suite.setStartTime(2l);
        suite.setEndTime(3l);
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
        testCase.setDuration(20l);

        suite.getTestCases().add(testCase);

        return result;
    }

    private int intVal(long value) {
        return Long.valueOf(value).intValue();
    }

}
