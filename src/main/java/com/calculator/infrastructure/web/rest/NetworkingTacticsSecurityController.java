package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.rps.RequestPerSecondCostCalculatorService;
import com.calculator.application.services.mapper.tradeoffs.reliability.ReliabilityTradeoffMapperService;
import com.calculator.application.services.mapper.tradeoffs.resiliency.ResiliencyTradeoffMapperService;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTradeoffDTO;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import com.calculator.application.services.mapper.tradeoffs.security.SecurityTradeoffMapperService;
import com.calculator.domain.dto.requests.EffectiveRequestPerSecondRequest;
import com.calculator.domain.dto.responses.EffectiveRequestPerSecondResponse;
import com.calculator.domain.dto.tactics.security.authentication.BasicAuthenticationPattern;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.dto.tactics.security.tls.TLSTactic;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class NetworkingTacticsSecurityController {

    @Autowired
    SecurityTradeoffMapperService securityTradeoffMapperService;

    @Autowired
    ReliabilityTradeoffMapperService reliabilityTradeoffMapperService;

    @Autowired
    ResiliencyTradeoffMapperService resiliencyTradeoffMapperService;

    @Autowired
    RequestPerSecondCostCalculatorService requestPerSecondCostCalculatorService;

    @PostMapping("/api/tco/effective-rps")
    public ResponseEntity<EffectiveRequestPerSecondResponse> calculateEffectiveRps(
            @RequestBody EffectiveRequestPerSecondRequest effectiveRpsRequest) {
        return getEffectiveRequestPerSecondResponseResponseEntity(effectiveRpsRequest);
    }

    /**
    private ResponseEntity<EffectiveRequestPerSecondResponse> getEffectiveRequestPerSecondResponseResponseEntity(EffectiveRequestPerSecondRequest effectiveRpsRequest) {
        EffectiveRequestPerSecondResponse effectiveRpsResponse = new EffectiveRequestPerSecondResponse();
        effectiveRpsResponse.setBaseRps(effectiveRpsRequest.getBaseRequestPerSecond());

        if (effectiveRpsRequest.getBaseRequestPerSecond() <= 0) {
            effectiveRpsResponse.setEffectiveRps(0);
            effectiveRpsResponse.setRpsWasAdjusted(false);
            return ResponseEntity.ok(effectiveRpsResponse);
        }

        int effective = effectiveRpsRequest.getBaseRequestPerSecond();

        // ── 2. Retry (always uses BASE RPS, never SAGA-multiplied) ───────────
        if (effectiveRpsRequest.isRetryEnabled()) {
            int extra = (int) Math.round(effectiveRpsRequest.getBaseRequestPerSecond() * effectiveRpsRequest.getRetryErrorPercentage() / 100.0);
            effective += extra;
            effectiveRpsResponse.setRetryExtra(extra);
            effectiveRpsResponse.getBreakdown().add("+" + extra + " Retry (" + effectiveRpsRequest.getRetryErrorPercentage()
                    + "% \u00d7 " + effectiveRpsRequest.getBaseRequestPerSecond()+ " base RPS = " + extra + " retry/s)");
        }

        // ── 3. TLS / mTLS handshakes ─────────────────────────────────────────
        if (effectiveRpsRequest.isTlsEnabled() || effectiveRpsRequest.isMtlsEnabled()) {
            int msgs  = effectiveRpsRequest.isMtlsEnabled() ? 5 : 2;
            int handshakeRps = (int) Math.round(effectiveRpsRequest.getTlsReconnectsPerHour() * msgs / 3600.0);
            if (handshakeRps > 0) {
                effective += handshakeRps;
                effectiveRpsResponse.setHandshakeExtra(handshakeRps);
                effectiveRpsResponse.getBreakdown().add("+" + handshakeRps + " "
                        + (effectiveRpsRequest.isMtlsEnabled() ? "mTLS" : "TLS") + " handshakes");
            }
        }

        // ── 4. OAuth 2.0 token acquisition / remote introspection ────────────
        if (effectiveRpsRequest.isOauthEnabled()) {
            int ttl = effectiveRpsRequest.getTokenTtlSeconds()  > 0 ? effectiveRpsRequest.getTokenTtlSeconds()  : 3600;
            int clients = effectiveRpsRequest.getConcurrentClients() > 0 ? effectiveRpsRequest.getConcurrentClients() : 1;

            int tokenAcq   = (int) Math.round((double) effectiveRpsRequest.getBaseRequestPerSecond() / (ttl * clients));
            int introspRps = OAuthTokenValidationModes.REMOTE_INTROSPECTION.name().equals(effectiveRpsRequest.getTokenValidationMode()) ? effectiveRpsRequest.getBaseRequestPerSecond() : 0;

            effective += tokenAcq + introspRps;
            effectiveRpsResponse.setTokenAcqExtra(tokenAcq);
            effectiveRpsResponse.setIntrospectionExtra(introspRps);

            if (tokenAcq > 0)   effectiveRpsResponse.getBreakdown().add("+" + tokenAcq + " token acq/s");
            if (introspRps > 0) effectiveRpsResponse.getBreakdown().add("+" + introspRps + " remote introspection/s");

            effectiveRpsResponse.setOauthPreview(
                    effectiveRpsRequest.getBaseRequestPerSecond() + " base"
                    + (introspRps > 0 ? " + " + introspRps + " intr" : "")
                    + (tokenAcq  > 0 ? " + " + tokenAcq  + " acq"  : "")
                    + " = " + effective + " req/s"
                    );
        }

        effectiveRpsResponse.setEffectiveRps(effective);
        effectiveRpsResponse.setRpsWasAdjusted((effective != effectiveRpsRequest.getBaseRequestPerSecond()));
        return ResponseEntity.ok(effectiveRpsResponse);
    }
     */

    private ResponseEntity<EffectiveRequestPerSecondResponse> getEffectiveRequestPerSecondResponseResponseEntity(
            EffectiveRequestPerSecondRequest effectiveRpsRequest) {

        EffectiveRequestPerSecondResponse effectiveRpsResponse = new EffectiveRequestPerSecondResponse();
        effectiveRpsResponse.setBaseRps(effectiveRpsRequest.getBaseRequestPerSecond());

        // Fail-fast guard for baseline or invalid traffic
        if (effectiveRpsRequest.getBaseRequestPerSecond() <= 0) {
            effectiveRpsResponse.setEffectiveRps(0);
            effectiveRpsResponse.setRpsWasAdjusted(false);
            return ResponseEntity.ok(effectiveRpsResponse);
        }

        // 1. ADAPT: Map flat incoming request properties to the nested domain record architecture
        RetryPattern retryPattern = new RetryPattern(
                effectiveRpsRequest.isRetryEnabled(),
                effectiveRpsRequest.getRetryErrorPercentage()
        );

        InterceptorType interceptorType = effectiveRpsRequest.getInterceptorType().equals(InterceptorType.UNARY.name()) ? InterceptorType.UNARY : InterceptorType.STREAM;
        OAuthTokenValidationModes oAuthTokenValidationMode = effectiveRpsRequest.getTokenValidationMode().equals(OAuthTokenValidationModes.LOCAL.name()) ? OAuthTokenValidationModes.LOCAL : OAuthTokenValidationModes.REMOTE_INTROSPECTION;

        SecurityTactics securityTactics = new SecurityTactics(
                new TLSTactic(
                        effectiveRpsRequest.isTlsEnabled(),
                        effectiveRpsRequest.isMtlsEnabled(),
                        effectiveRpsRequest.getTlsReconnectsPerHour()
                ),
                new JWTTactic(
                        effectiveRpsRequest.isOauthEnabled(),
                        oAuthTokenValidationMode,
                        effectiveRpsRequest.getTokenTtlSeconds(),
                        effectiveRpsRequest.getConcurrentClients(),
                        interceptorType
                ),
                new BasicAuthenticationPattern(
                        false // TODO verify how to extract it
                )
        );

        ArchitecturalDecisionsDTO architecturalDecisions = new ArchitecturalDecisionsDTO(
                effectiveRpsRequest.getBaseRequestPerSecond(),
                ReliabilityTactics.empty(),
                TimeoutPattern.empty(),
                retryPattern,
                CircuitBreakerPattern.empty(),
                securityTactics
        );

        long coreCalculatedEffectiveRps = requestPerSecondCostCalculatorService.calculateEffectiveRequestsPerSecond(architecturalDecisions);

        int totalExtra = (int) (coreCalculatedEffectiveRps - effectiveRpsRequest.getBaseRequestPerSecond());

        if (totalExtra > 0) {
            // Only run UI calculations if traffic overhead actually exists
            int baseRps = effectiveRpsRequest.getBaseRequestPerSecond();
            int securityOverhead = 0;

            // A. Calculate Security Overhead First (TLS & OAuth Components)
            int handshakeRps = 0;
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

            int tokenAcq = 0;
            int introspRps = 0;
            if (effectiveRpsRequest.isOauthEnabled()) {
                int ttl = effectiveRpsRequest.getTokenTtlSeconds() > 0 ? effectiveRpsRequest.getTokenTtlSeconds() : 3600;
                int clients = effectiveRpsRequest.getConcurrentClients() > 0 ? effectiveRpsRequest.getConcurrentClients() : 1;

                tokenAcq = (int) Math.round((double) baseRps / (ttl * clients));
                introspRps = OAuthTokenValidationModes.REMOTE_INTROSPECTION.name().equals(effectiveRpsRequest.getTokenValidationMode()) ? baseRps : 0;

                effectiveRpsResponse.setTokenAcqExtra(tokenAcq);
                effectiveRpsResponse.setIntrospectionExtra(introspRps);

                if (tokenAcq > 0) effectiveRpsResponse.getBreakdown().add("+" + tokenAcq + " token acq/s");
                if (introspRps > 0) effectiveRpsResponse.getBreakdown().add("+" + introspRps + " remote introspection/s");

                securityOverhead += tokenAcq + introspRps;
            }

            // B. Define total traffic exposed to potential network failures
            int trafficSubjectToFailure = baseRps + securityOverhead;

            // C. Calculate Retry Delta using the compounded traffic baseline
            if (effectiveRpsRequest.isRetryEnabled()) {
                int extraRetry = (int) Math.round(trafficSubjectToFailure * (effectiveRpsRequest.getRetryErrorPercentage() / 100.0));
                System.out.println("Extra retry (compounded): " + extraRetry);
                effectiveRpsResponse.setRetryExtra(extraRetry);
                effectiveRpsResponse.getBreakdown().add("+" + extraRetry + " Retry (" + effectiveRpsRequest.getRetryErrorPercentage()
                        + "% \u00d7 " + trafficSubjectToFailure + " traffic subject to failure = " + extraRetry + " retry/s)");
            }

            // D. Update Oauth UI Preview String to match the new formula execution sequence
            if (effectiveRpsRequest.isOauthEnabled()) {
                effectiveRpsResponse.setOauthPreview(
                        baseRps + " base"
                                + (introspRps > 0 ? " + " + introspRps + " intr" : "")
                                + (tokenAcq > 0 ? " + " + tokenAcq + " acq" : "")
                                + " = " + coreCalculatedEffectiveRps + " req/s"
                );
            }
        } else {
            // Safe fallback string when there is zero architectural overhead
            effectiveRpsResponse.setOauthPreview(effectiveRpsRequest.getBaseRequestPerSecond() + " base = " + coreCalculatedEffectiveRps + " req/s");
        }

        // Assign final payload states
        effectiveRpsResponse.setEffectiveRps((int) coreCalculatedEffectiveRps);
        System.out.println("coreCalculatedEffectiveRps: " + coreCalculatedEffectiveRps);
        effectiveRpsResponse.setRpsWasAdjusted(totalExtra != 0);

        return ResponseEntity.ok(effectiveRpsResponse);
    }

    @GetMapping("/api/security/tactic-mappings")
    public ResponseEntity<List<SecurityTradeoffsDTO>> getTacticSecurityMappings() {
        System.out.println("Security Tactics Mapping has been invoked");
        List<SecurityTradeoffsDTO> securityMappings = securityTradeoffMapperService.getSecurityTradeoffs();
        return ResponseEntity.ok(securityMappings);
    }

    @GetMapping("/api/reliability/tactic-mappings")
    public ResponseEntity<List<ReliabilityTradeoffDTO>> getTacticReliabilityMappings() {
        System.out.println("Reliability Tactics Mapping has been invoked");
        List<ReliabilityTradeoffDTO> reliabilityMappings = reliabilityTradeoffMapperService.getReliabilityTradeoffs();
        return ResponseEntity.ok(reliabilityMappings);
    }

    @GetMapping("/api/resiliency/tactic-mappings")
    public ResponseEntity<List<ResiliencyTradeoffDTO>> getTacticResiliencyMappings() {
        System.out.println("Resiliency Tactics Mapping has been invoked");
        List<ResiliencyTradeoffDTO> reliabilityMappings = resiliencyTradeoffMapperService.getResiliencyTradeoffs();
        return ResponseEntity.ok(reliabilityMappings);
    }

}