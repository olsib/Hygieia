package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.editors.CaseInsensitiveTestSuiteTypeEditor;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.request.PerfTestDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import com.capitalone.dashboard.request.CustodianResultRequest;
import com.capitalone.dashboard.service.CustodianResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class CustodianResultController {
    private final CustodianResultService CustodianResultService;

    @Autowired
    public CustodianResultController(CustodianResultService CustodianResultService) {
        this.CustodianResultService = CustodianResultService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(CodeQualityType.class, new CaseInsensitiveTestSuiteTypeEditor());
    }

    @RequestMapping(value = "/quality/test", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<CustodianResult>> qualityData(@Valid CustodianResultRequest request) {
        return CustodianResultService.search(request);
    }


    @RequestMapping(value = "/quality/test", method = POST,
                consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
        public ResponseEntity<String> createTest(@Valid @RequestBody TestDataCreateRequest request) throws HygieiaException {
            String response = CustodianResultService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
    }

    @RequestMapping(value = "/v2/quality/test", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTestV2(@Valid @RequestBody TestDataCreateRequest request) throws HygieiaException {
        String response = CustodianResultService.createV2(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/quality/CustodianResult", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createPerfTest(@Valid @RequestBody PerfTestDataCreateRequest request) throws HygieiaException {
        String response = CustodianResultService.createPerf(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/v2/quality/CustodianResult", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createPerfTestV2(@Valid @RequestBody PerfTestDataCreateRequest request) throws HygieiaException {
        String response = CustodianResultService.createPerfV2(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
