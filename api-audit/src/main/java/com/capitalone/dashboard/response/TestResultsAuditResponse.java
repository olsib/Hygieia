package com.capitalone.dashboard.response;

import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.Traceability;
import com.capitalone.dashboard.status.CustodianResultAuditStatus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustodianResultsAuditResponse extends AuditReviewResponse<CustodianResultAuditStatus> {
    private String url;
    private long lastExecutionTime;
    private Collection<TestCapability> testCapabilities;
    private String type;
    private Map featureCustodianResult = new HashMap();
    public Traceability traceability;

    public Collection<TestCapability> getTestCapabilities() { return testCapabilities; }

    public void setTestCapabilities(Collection<TestCapability> testCapabilities) { this.testCapabilities = testCapabilities; }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(long lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public Map getFeatureCustodianResult() { return featureCustodianResult; }

    public void setFeatureCustodianResult(Map featureCustodianResult) { this.featureCustodianResult = featureCustodianResult; }

    public Traceability getTraceability() { return traceability; }

    public void setTraceability(Traceability traceability) { this.traceability = traceability; }
}