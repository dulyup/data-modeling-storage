package com.neu.demo.controller;

import com.google.common.base.Strings;
import com.neu.demo.repository.PlanRepository;
import com.neu.demo.security.AuthenticateService;
import com.neu.demo.util.JsonMapUtil;
import com.nimbusds.jose.JOSEException;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;


/**
 * @author lyupingdu
 * @date 2019-07-24.
 */
@RestController
@RequestMapping("/plans")
public class PlanController {

    private PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity getById(@PathVariable("id") final String id,
                                  HttpServletRequest request) throws ParseException, JOSEException {
        boolean verified = AuthenticateService.authenticate(request);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Authentication failed");
        }
        val res = planRepository.findById(id);
        if (res != null && res.size() != 0) {
            return ResponseEntity.status(HttpStatus.OK).body(res);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No date found with the provided id");
    }

    @PostMapping()
    public ResponseEntity addNew(@RequestBody String body,
                                 HttpServletRequest request) throws ParseException, JOSEException, IOException {
        boolean verified = AuthenticateService.authenticate(request);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Authentication failed");
        }
        boolean isValid = JsonMapUtil.validateJson(body);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Input validation against schema failed");
        }
        val id = planRepository.addNew(body);
        if (id != null) {
            return ResponseEntity.status(HttpStatus.OK).body("Adding succeeded. Id: " + id);
        }
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Adding failed");
    }

    @PatchMapping(value = "/{id}")
    public ResponseEntity patchById(@PathVariable("id") final String id,
                                    @RequestBody String body,
                                    HttpServletRequest request) throws ParseException, JOSEException, IOException {
        boolean verified = AuthenticateService.authenticate(request);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Authentication failed");
        }
        planRepository.partialUpdateById(id, body);
        return ResponseEntity.status(HttpStatus.OK).body("Updating succeeded. Id: " + id);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity putById(@PathVariable("id") final String id,
                                  @RequestBody String body,
                                  HttpServletRequest request) throws ParseException, JOSEException, IOException {
        boolean verified = AuthenticateService.authenticate(request);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Authentication failed");
        }
        planRepository.fullUpdateById(id, body);
        return ResponseEntity.status(HttpStatus.OK).body("Updating succeeded. Id: " + id);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity deleteById(@PathVariable("id") final String id,
                                     HttpServletRequest request) throws ParseException, JOSEException, IOException {
        boolean verified = AuthenticateService.authenticate(request);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Authentication failed");
        }
        planRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.OK).body("Deleting succeeded. Id: " + id);
    }

    @GetMapping
    public ResponseEntity getAll(HttpServletRequest request) throws ParseException, JOSEException, IOException {
        boolean verified = AuthenticateService.authenticate(request);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Authentication failed");
        }
        val allPlans = planRepository.getAll();
        if (allPlans != null) {
            return ResponseEntity.status(HttpStatus.OK).body(allPlans);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(allPlans);
    }
}
