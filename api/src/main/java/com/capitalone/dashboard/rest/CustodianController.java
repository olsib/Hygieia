package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.editors.CaseInsensitiveTestSuiteTypeEditor;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.CustodianResult;
import com.capitalone.dashboard.request.CustodianRequest;
import com.capitalone.dashboard.service.CustodianService;
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
public class CustodianController {
    private final CustodianService custodianService;

    @Autowired
    public CustodianController(CustodianService custodianService) {
        this.custodianService = custodianService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(CodeQualityType.class, new CaseInsensitiveTestSuiteTypeEditor());
    }

    @RequestMapping(value = "/custodian/metrics", method = GET, produces = APPLICATION_JSON_VALUE)
    public DataResponse<Iterable<CustodianResult>> qualityData(@Valid CustodianRequest request) {
        return custodianService.search(request);
    }

    @RequestMapping(value = "/custodian/metrics", method = POST,
                consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
        public ResponseEntity<String> createTest(@Valid @RequestBody CustodianDataCreateRequest request) throws HygieiaException {
            String response = custodianService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
    }
}
