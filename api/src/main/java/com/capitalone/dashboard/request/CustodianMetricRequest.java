package com.capitalone.dashboard.request;

import org.bson.types.ObjectId;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;

public class CustodianMetricRequest {
    @NotNull
    private ObjectId collectorItemId;
    private Long timestamp;
    private String configurationItem;
    private String policy;
    private String resource;
    private Long account_id;
    private String region;
    private Integer count;

    public ObjectId getComponentId() {
        return collectorItemId;
    }

    public void setComponentId(ObjectId collectorItemId) {
        this.collectorItemId = collectorItemId;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getConfigurationItem() {
        return configurationItem;
    }
    public void setConfigurationItem(String configurationItem) {
        this.configurationItem = configurationItem;
    }

    public String getPolicy() {
        return policy;
    }
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getAccountid() {
        return account_id;
    }
    public void setAccountId(Long account_id) {
        this.account_id = account_id;
    }

    public String getRegion() {
        return region;
    }
    public void setRegion(Long region) { this.region = region; }

    public Integer getCount() {
        return count;
    }
    public void setCount(Integer count) { this.count = count; }




