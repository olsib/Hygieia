package hygieia.transformer;

import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.model.TestSuiteType;
import com.capitalone.dashboard.model.quality.CheckstyleReport;
import com.capitalone.dashboard.model.quality.CucumberJsonReport;
import com.capitalone.dashboard.model.quality.FindBugsXmlReport;
import com.capitalone.dashboard.model.quality.JacocoXmlReport;
import com.capitalone.dashboard.model.quality.JunitXmlReport;
import com.capitalone.dashboard.model.quality.MochaJsSpecReport;
import com.capitalone.dashboard.model.quality.PmdReport;
import com.capitalone.dashboard.model.quality.QualityVisitor;
import com.capitalone.dashboard.request.BuildDataCreateRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stevegal on 2019-03-25.
 */
public class CustodianResultVisitor implements QualityVisitor<CustodianResult> {

    private List<TestCapability> capabilities = new ArrayList<>();

    private String testType;
    private BuildDataCreateRequest buildDataCreateRequest;
    private String capabilityDescription;

    public CustodianResultVisitor(String testType, BuildDataCreateRequest buildDataCreateRequest) {
        this.testType = testType;
        this.buildDataCreateRequest = buildDataCreateRequest;
    }

    @Override
    public CustodianResult produceResult() {
        return this.buildCustodianResultObject(this.capabilities, this.buildDataCreateRequest, this.testType);
    }

    @Override
    public void visit(JunitXmlReport junitXmlReport) {
        // no impl... could expand
    }

    @Override
    public void visit(FindBugsXmlReport findBugsXmlReport) {
        // no impl... could expand
    }

    @Override
    public void visit(JacocoXmlReport jacocoXmlReport) {
        // no impl... could expand
    }

    @Override
    public void visit(PmdReport pmdReport) {
        // no impl... could expand
    }

    @Override
    public void visit(CheckstyleReport checkstyleReport) {
        // no impl... could expandÒ
    }

    @Override
    public void visit(MochaJsSpecReport mochaJsSpecReport) {
        MochaSpecToTestCapabilityTransformer transformer = new MochaSpecToTestCapabilityTransformer(this.buildDataCreateRequest, this.capabilityDescription);
        TestCapability capability = transformer.convert(mochaJsSpecReport);
        this.capabilities.add(capability);
    }

    @Override
    public void visit(CucumberJsonReport cucumberJsonReport) {
        CucumberJsonToTestCapabilityTransformer transformer = new CucumberJsonToTestCapabilityTransformer(this.buildDataCreateRequest, this.capabilityDescription);
        TestCapability capability = transformer.convert(cucumberJsonReport);
        this.capabilities.add(capability);
    }

    private CustodianResult buildCustodianResultObject(List<TestCapability> capabilities, BuildDataCreateRequest buildDataCreateRequest, String testType) {
        if (!capabilities.isEmpty()) {
            // There are test suites so let's construct a CustodianResult to encapsulate these results
            CustodianResult CustodianResult = new CustodianResult();
            CustodianResult.setType(TestSuiteType.fromString(testType));
            CustodianResult.setDescription(buildDataCreateRequest.getJobName());
            CustodianResult.setExecutionId(String.valueOf(buildDataCreateRequest.getNumber()));
            CustodianResult.setUrl(buildDataCreateRequest.getBuildUrl() + String.valueOf(buildDataCreateRequest.getNumber()) + "/");
            CustodianResult.setDuration(buildDataCreateRequest.getDuration());
            CustodianResult.setEndTime(buildDataCreateRequest.getStartTime() + buildDataCreateRequest.getDuration());
            CustodianResult.setStartTime(buildDataCreateRequest.getStartTime());
            CustodianResult.getTestCapabilities().addAll(capabilities);  //add all capabilities
            CustodianResult.setTotalCount(capabilities.size());
            CustodianResult.setTimestamp(System.currentTimeMillis());
            int testCapabilitySkippedCount = 0, testCapabilitySuccessCount = 0, testCapabilityFailCount = 0;
            int testCapabilityUnknownCount = 0;
            // Calculate counts based on test suites
            for (TestCapability cap : capabilities) {
                switch (cap.getStatus()) {
                    case Success:
                        testCapabilitySuccessCount++;
                        break;
                    case Failure:
                        testCapabilityFailCount++;
                        break;
                    case Skipped:
                        testCapabilitySkippedCount++;
                        break;
                    default:
                        testCapabilityUnknownCount++;
                        break;
                }
            }
            CustodianResult.setSuccessCount(testCapabilitySuccessCount);
            CustodianResult.setFailureCount(testCapabilityFailCount);
            CustodianResult.setSkippedCount(testCapabilitySkippedCount);
            CustodianResult.setUnknownStatusCount(testCapabilityUnknownCount);
            return CustodianResult;
        }
        return null;
    }

    public void setCurrentDescriprion(String capabilityDescription) {
        this.capabilityDescription = capabilityDescription;
    }
}
