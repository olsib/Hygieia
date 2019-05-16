package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.CustodianMetrics;
import com.capitalone.dashboard.repository.CustodianMetricsRepository;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustodianMetricServiceImpl implements CustodianMetricsService {

    private final CustodianMetricsRepository CustodianMetricsRepository;

    @Autowired
    public CustodianMetricServiceImpl(CustodianMetricsRepository CustodianMetricsRepository) {
        this.CustodianMetricsRepository = CustodianMetricsRepository;

    }

    @Override
    public Page<CustodianMetric> configurationItemsByTypeWithFilter(String itemType, String filter, Pageable pageable) {

        Page<CustodianMetric> configItemString;

        if( StringUtils.isNotEmpty( filter ) ){

            List<CustodianMetric> CustodianMetricList = CustodianMetricsRepository.findAllByConfigurationItemContainingOrCommonNameContainingAllIgnoreCase(filter,filter);

            List<ObjectId> CustodianMetricIdsList = new ArrayList<>();
            for (CustodianMetric CustodianMetric : CustodianMetricList) {
                CustodianMetricIdsList.add(CustodianMetric.getId());
            }

            configItemString = CustodianMetricsRepository.findAllByItemTypeAndValidConfigItemAndIdIn(itemType,true, CustodianMetricIdsList, pageable);

        }else{
            configItemString = CustodianMetricsRepository.findAllByItemTypeAndConfigurationItemContainingIgnoreCaseAndValidConfigItem(
                    itemType, filter, pageable, true);
        }
        return configItemString;
    }
    @Override
    public String configurationItemNameByObjectId(ObjectId objectId){
        CustodianMetric CustodianMetric = configurationItemsByObjectId(objectId);
        return CustodianMetric.getConfigurationItem();
    }
    @Override
    public CustodianMetric configurationItemsByObjectId(ObjectId objectId){
        CustodianMetric CustodianMetric = CustodianMetricsRepository.findOne(objectId);
        return CustodianMetric;
    }
    @Override
    public CustodianMetric configurationItemByConfigurationItem(String configItem){
        CustodianMetric CustodianMetricItem= CustodianMetricsRepository.findByConfigurationItemIgnoreCase(configItem);
        return CustodianMetricItem;
    }
    @Override
    public List<CustodianMetric> getAllBusServices(){
        List<CustodianMetric> CustodianMetrics = CustodianMetricsRepository.findAllByItemType("app");
        return CustodianMetrics;
    }
}
