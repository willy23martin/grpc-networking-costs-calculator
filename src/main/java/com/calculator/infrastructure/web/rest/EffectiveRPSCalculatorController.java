package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.rps.RequestPerSecondCostCalculatorService;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.requests.EffectiveRequestPerSecondRequest;
import com.calculator.domain.dto.responses.EffectiveRequestPerSecondResponse;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

import static com.calculator.application.services.mapper.ArchitecturalDecisionsDTOMapper.mapToArchitecturalDecisionsDTO;

@RestController
@CrossOrigin(origins = "*")
public class EffectiveRPSCalculatorController {

    private final Logger log = Logger.getLogger(EffectiveRPSCalculatorController.class.getName());

    @Autowired
    private RequestPerSecondCostCalculatorService requestPerSecondCostCalculatorService;

    @PostMapping("/api/tco/effective-rps")
    public ResponseEntity<EffectiveRequestPerSecondResponse> calculateEffectiveRps(
            @RequestBody EffectiveRequestPerSecondRequest effectiveRpsRequest) {
        return getEffectiveRequestPerSecondResponseResponseEntity(effectiveRpsRequest);
    }

    private ResponseEntity<EffectiveRequestPerSecondResponse> getEffectiveRequestPerSecondResponseResponseEntity(
            EffectiveRequestPerSecondRequest effectiveRpsRequest) {

        EffectiveRequestPerSecondResponse effectiveRpsResponse = new EffectiveRequestPerSecondResponse();
        effectiveRpsResponse.setBaseRps(effectiveRpsRequest.getBaseRequestPerSecond());

        if (effectiveRpsRequest.getBaseRequestPerSecond() <= 0) {
            effectiveRpsResponse.setEffectiveRps(0);
            effectiveRpsResponse.setRpsWasAdjusted(false);
            return ResponseEntity.ok(effectiveRpsResponse);
        }

        ArchitecturalDecisionsDTO architecturalDecisions = mapToArchitecturalDecisionsDTO(effectiveRpsRequest);
        long coreCalculatedEffectiveRps = requestPerSecondCostCalculatorService.calculateEffectiveRequestsPerSecond(architecturalDecisions);
        int extraRequestsPerSecond = (int) (coreCalculatedEffectiveRps - effectiveRpsRequest.getBaseRequestPerSecond());

        if (extraRequestsPerSecond > 0) {
            int baseRps = effectiveRpsRequest.getBaseRequestPerSecond();
            int securityOverhead = 0;
            int tokenAcquisitionFactor = 0;
            int tokenRemoteIntrospectionValidationModeRequestPerSecond = 0;

            securityOverhead = getTLSSecurityOverhead(effectiveRpsRequest, effectiveRpsResponse, securityOverhead);
            if (effectiveRpsRequest.isOauthEnabled()) {
                int oauthTokenTTL = effectiveRpsRequest.getTokenTtlSeconds() > 0 ? effectiveRpsRequest.getTokenTtlSeconds() : 3600;
                int oauthConcurrentClients = effectiveRpsRequest.getConcurrentClients() > 0 ? effectiveRpsRequest.getConcurrentClients() : 1;

                tokenAcquisitionFactor = (int) Math.round((double) baseRps / (oauthTokenTTL * oauthConcurrentClients));
                tokenRemoteIntrospectionValidationModeRequestPerSecond = OAuthTokenValidationModes.REMOTE_INTROSPECTION.name().equals(effectiveRpsRequest.getTokenValidationMode()) ? baseRps : 0;

                effectiveRpsResponse.setTokenAcqExtra(tokenAcquisitionFactor);
                effectiveRpsResponse.setIntrospectionExtra(tokenRemoteIntrospectionValidationModeRequestPerSecond);

                if (tokenAcquisitionFactor > 0) effectiveRpsResponse.getBreakdown().add("+" + tokenAcquisitionFactor + " token acq/s");
                if (tokenRemoteIntrospectionValidationModeRequestPerSecond > 0) effectiveRpsResponse.getBreakdown().add("+" + tokenRemoteIntrospectionValidationModeRequestPerSecond + " remote introspection/s");

                securityOverhead += tokenAcquisitionFactor + tokenRemoteIntrospectionValidationModeRequestPerSecond;
            }

            int trafficSubjectToFailure = baseRps + securityOverhead;
            calculateRetryDeltaUsingCompoundedTrafficBaseline(effectiveRpsRequest, trafficSubjectToFailure, effectiveRpsResponse);
            updateOauthUIPreviewContentToMatchFormulaSequence(effectiveRpsRequest, effectiveRpsResponse, baseRps, tokenRemoteIntrospectionValidationModeRequestPerSecond, tokenAcquisitionFactor, coreCalculatedEffectiveRps);
        } else {
            setEffectiveRpsResponseWhenNoOverheadIsAdded(effectiveRpsResponse, effectiveRpsRequest.getBaseRequestPerSecond() + " base = " + coreCalculatedEffectiveRps + " req/s");
        }

        set(effectiveRpsResponse, coreCalculatedEffectiveRps, extraRequestsPerSecond);

        return ResponseEntity.ok(effectiveRpsResponse);
    }

    private void calculateRetryDeltaUsingCompoundedTrafficBaseline(EffectiveRequestPerSecondRequest effectiveRpsRequest, int trafficSubjectToFailure, EffectiveRequestPerSecondResponse effectiveRpsResponse) {
        if (effectiveRpsRequest.isRetryEnabled()) {
            int extraRetry = (int) Math.round(trafficSubjectToFailure * (effectiveRpsRequest.getRetryErrorPercentage() / 100.0));
            log.info("Extra retry (compounded): " + extraRetry);
            effectiveRpsResponse.setRetryExtra(extraRetry);
            effectiveRpsResponse.getBreakdown().add("+" + extraRetry + " Retry (" + effectiveRpsRequest.getRetryErrorPercentage()
                    + "% × " + trafficSubjectToFailure + " traffic subject to failure = " + extraRetry + " retry/s)");
        }
    }

    private void updateOauthUIPreviewContentToMatchFormulaSequence(EffectiveRequestPerSecondRequest effectiveRpsRequest, EffectiveRequestPerSecondResponse effectiveRpsResponse, int baseRps, int introspRps, int tokenAcq, long coreCalculatedEffectiveRps) {
        if (effectiveRpsRequest.isOauthEnabled()) {
            setEffectiveRpsResponseWhenNoOverheadIsAdded(effectiveRpsResponse, baseRps + " base"
                    + (introspRps > 0 ? " + " + introspRps + " intr" : "")
                    + (tokenAcq > 0 ? " + " + tokenAcq + " acq" : "")
                    + " = " + coreCalculatedEffectiveRps + " req/s");
        }
    }

    private void setEffectiveRpsResponseWhenNoOverheadIsAdded(EffectiveRequestPerSecondResponse effectiveRpsResponse, String effectiveRpsRequest) {
        effectiveRpsResponse.setOauthPreview(effectiveRpsRequest);
    }

    private void set(EffectiveRequestPerSecondResponse effectiveRpsResponse, long coreCalculatedEffectiveRps, int totalExtra) {
        effectiveRpsResponse.setEffectiveRps((int) coreCalculatedEffectiveRps);
        log.info("coreCalculatedEffectiveRps: " + coreCalculatedEffectiveRps);
        log.info("TOTAL EXTRA: " + totalExtra);
        effectiveRpsResponse.setRpsWasAdjusted(totalExtra != 0);
        log.info("effectiveRpsResponse: " + totalExtra);
    }

    public int getTLSSecurityOverhead(EffectiveRequestPerSecondRequest effectiveRpsRequest, EffectiveRequestPerSecondResponse effectiveRpsResponse, int securityOverhead) {
        int handshakeRps;
        if (effectiveRpsRequest.isTlsEnabled() || effectiveRpsRequest.isMtlsEnabled()) {
            int msgs = effectiveRpsRequest.isMtlsEnabled() ? 5 : 2;
            handshakeRps = (int) Math.round(effectiveRpsRequest.getTlsReconnectsPerHour() * msgs / 3600.0);
            if (handshakeRps > 0) {
                effectiveRpsResponse.setHandshakeExtra(handshakeRps);
                effectiveRpsResponse.getBreakdown().add("+" + handshakeRps + " "
                        + (effectiveRpsRequest.isMtlsEnabled() ? "mTLS" : "TLS") + " handshakes");
                securityOverhead += handshakeRps;
            }
        }
        return securityOverhead;
    }

}