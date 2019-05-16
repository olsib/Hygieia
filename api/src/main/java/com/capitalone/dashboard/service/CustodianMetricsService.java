package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.CustodianMetrics;
import com.capitalone.dashboard.request.CustodianRequest;

public interface CustodianMetricsService {

    DataResponse<Iterable<CustodianMetrics>> search(CustodianRequest request);
    String create(CustodianRequest request) throws HygieiaException;

}
