package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import com.calculator.application.services.mapper.tradeoffs.security.SecurityTradeoffMapperService;
import com.calculator.domain.dto.requests.EffectiveRequestPerSecondRequest;
import com.calculator.domain.dto.requests.EffectiveRequestPerSecondResponse;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class NetworkingTacticsSecurityController {

    @Autowired
    private SecurityTradeoffMapperService securityTradeoffMapperService;

    @PostMapping("/api/tco/effective-rps")
    public ResponseEntity<EffectiveRequestPerSecondResponse> calculateEffectiveRps(
            @RequestBody EffectiveRequestPerSecondRequest effectiveRpsRequest) {
        return getEffectiveRequestPerSecondResponseResponseEntity(effectiveRpsRequest);
    }

    private static ResponseEntity<EffectiveRequestPerSecondResponse> getEffectiveRequestPerSecondResponseResponseEntity(EffectiveRequestPerSecondRequest effectiveRpsRequest) {
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

    @GetMapping("/api/security/tactic-mappings")
    public ResponseEntity<List<SecurityTradeoffsDTO>> getTacticSecurityMappings() {
        System.out.println("Security Tactics Mapping has been invoked");
        List<SecurityTradeoffsDTO> securityMappings = securityTradeoffMapperService.getSecurityTradeoffs();
        return ResponseEntity.ok(securityMappings);
    }

}