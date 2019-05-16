package com.capitalone.dashboard.collector;

import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import com.capitalone.dashboard.CustodianResultSettings;
import com.capitalone.dashboard.core.client.JiraXRayRestClientSupplier;
import com.capitalone.dashboard.model.CustodianResultCollector;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.FeatureRepository;
import com.capitalone.dashboard.repository.CustodianResultRepository;
import com.capitalone.dashboard.repository.CustodianResultCollectorRepository;
import com.capitalone.dashboard.core.client.testexecution.TestExecutionClientImpl;
import com.capitalone.dashboard.util.CoreFeatureSettings;
import com.capitalone.dashboard.util.FeatureCollectorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Collects {@link CustodianResultCollector} data from feature content source system.
 */
@Component
public class CustodianResultCollectorTask extends CollectorTask<CustodianResultCollector> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustodianResultCollectorTask.class);

    private final CustodianResultRepository CustodianResultRepository;
    private final FeatureRepository featureRepository;
    private final CollectorItemRepository collectorItemRepository;
    private final CustodianResultCollectorRepository CustodianResultCollectorRepository;
    private final CustodianResultSettings CustodianResultSettings;
    private final JiraXRayRestClientSupplier restClientSupplier;
    DisposableHttpClient httpClient;

    CoreFeatureSettings coreFeatureSettings;

    /**
     * Default constructor for the collector task. This will construct this
     * collector task with all repository, scheduling, and settings
     * configurations custom to this collector.
     *
     * @param taskScheduler
     *            A task scheduler artifact
     * @param CustodianResultSettings
     *            The settings being used for feature collection from the source
     *            system
     */
    @Autowired
    public CustodianResultCollectorTask(CoreFeatureSettings coreFeatureSettings, TaskScheduler taskScheduler, CustodianResultRepository CustodianResultRepository,
                                   CustodianResultCollectorRepository CustodianResultCollectorRepository, CustodianResultSettings CustodianResultSettings,
                                   FeatureRepository featureRepository, CollectorItemRepository collectorItemRepository, JiraXRayRestClientSupplier restClientSupplier) {
        super(taskScheduler, FeatureCollectorConstants.JIRA_XRAY);
        this.CustodianResultRepository = CustodianResultRepository;
        this.CustodianResultCollectorRepository = CustodianResultCollectorRepository;
        this.coreFeatureSettings = coreFeatureSettings;
        this.CustodianResultSettings = CustodianResultSettings;
        this.featureRepository = featureRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.restClientSupplier = restClientSupplier;
        this.httpClient = httpClient;
    }

    /**
     * Accessor method for the collector prototype object
     */
    @Override
    public CustodianResultCollector getCollector() {
        return CustodianResultCollector.prototype();
    }

    /**
     * Accessor method for the collector repository
     */
    @Override
    public BaseCollectorRepository<CustodianResultCollector> getCollectorRepository() {
        return CustodianResultCollectorRepository;
    }

    /**
     * Accessor method for the current chronology setting, for the scheduler
     */
    @Override
    public String getCron() {
        return CustodianResultSettings.getCron();
    }

    /**
     * The collection action. This is the task which will run on a schedule to
     * gather data from the feature content source system and update the
     * repository with retrieved data.
     */
    @Override
    public void collect(CustodianResultCollector collector) {
        logBanner(CustodianResultSettings.getJiraBaseUrl());
        int count = 0;

        try {
            long testExecutionDataStart = System.currentTimeMillis();
            TestExecutionClientImpl testExecutionData = new TestExecutionClientImpl(this.CustodianResultRepository, this.CustodianResultCollectorRepository,
                    this.featureRepository, this.collectorItemRepository, this.CustodianResultSettings, this.restClientSupplier);
            count = testExecutionData.updateCustodianResultInformation();

            log("Test Execution Data", testExecutionDataStart, count);
            log("Finished", testExecutionDataStart);
        } catch (Exception e) {
            // catch exception here so we don't blow up the collector completely
            LOGGER.error("Failed to collect Jira XRay information", e);
        }
    }
}