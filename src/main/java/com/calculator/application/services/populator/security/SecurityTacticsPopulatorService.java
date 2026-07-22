package com.calculator.application.services.populator.security;

import com.calculator.application.services.calculators.rps.security.jwt.RPSJWTCostCalculator;
import com.calculator.application.services.mapper.tactics.TacticsMapperService;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.model.architecture.tactics.security.TLSOverhead;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.calculator.domain.model.architecture.tactics.security.JWTOverhead.JWT_OVERHEAD_BYTES_TYPICAL;
import static com.calculator.infrastructure.web.rest.NetworkingCostCalculatorController.DISPLAY_LOCALE;

@Service
public class SecurityTacticsPopulatorService {

    @Autowired
    TacticsMapperService tacticsMapperService;

    @Autowired
    RPSJWTCostCalculator rpsjwtCostCalculator;

    public void populate(SecurityTactics securityTactics, long baseRequestsPerSecond,
                         List<Map<String, String>> generalTactics,
                         List<Map<String, String>> requestsPerSecondModifierTactics) {
        if (securityTactics == null) return;
        if (securityTactics.tlsTactic().tlsEnabled()) {
            populateTLS(securityTactics, generalTactics, requestsPerSecondModifierTactics);
        }

        if (securityTactics.tlsTactic().mtlsEnabled()) {
            populateMTLS(securityTactics, generalTactics, requestsPerSecondModifierTactics);
        }

        if (securityTactics.basicAuthenticationPattern().basicAuthEnabled()) {
            generalTactics.add(tacticsMapperService.tacticEntry("Basic Authentication ⚠",
                    "~50–100 bytes per request header",
                    "Authorization: Basic base64(username:password) is sent with every request. " +
                            "No extra requests, but the credential is valid until the password changes — " +
                            "there is no token expiry or revocation mechanism. Not recommended for production gRPC APIs. " +
                            "Prefer OAuth 2.0 + JWT for time-bounded, revocable access control.", null));
        }

        if (securityTactics.jwtTactic().oauthJwtEnabled()) {
            populateOAuthJWTTactic(securityTactics, baseRequestsPerSecond, generalTactics, requestsPerSecondModifierTactics);
        }
    }

    private void populateOAuthJWTTactic(SecurityTactics securityTactics, long baseRequestsPerSecond, List<Map<String, String>> generalTactics, List<Map<String, String>> requestsPerSecondModifierTactics) {
        int ttl     = securityTactics.jwtTactic().tokenTtlSeconds()   > 0 ? securityTactics.jwtTactic().tokenTtlSeconds() : 3600;
        int clients = securityTactics.jwtTactic().concurrentClients()  > 0 ? securityTactics.jwtTactic().concurrentClients() : 1;

        String interceptorLabel = securityTactics.jwtTactic().interceptorType() == InterceptorType.UNARY
                ? "Unary interceptor" : "Stream interceptor";
        String jwtByteRange = String.format(
                "JWT Bearer header \n RFC 7515 Section-7.1 \n BASE64URL(UTF8(JWS Protected Header)) \n || '.' || * BASE64URL(JWS Payload) \n || '.' || * BASE64URL(JWS Signature)  \n adds %d bytes typical \n due to %s \n",
                JWT_OVERHEAD_BYTES_TYPICAL.getOverhead(), JWT_OVERHEAD_BYTES_TYPICAL.getFormattedReference()
        );

        long tokenAcqRps = rpsjwtCostCalculator.extraRequestsPerSecondFromOAuthTokenAcquisitionCallsToTheAuthorisationServer(baseRequestsPerSecond, securityTactics.jwtTactic());
        long introspectionRps = rpsjwtCostCalculator.extraRequestsPerSecondFromRemoteTokenIntrospection(baseRequestsPerSecond, securityTactics.jwtTactic());

        generalTactics.add(tacticsMapperService.tacticEntry("OAuth 2.0 + JWT — bearer token",
                interceptorLabel + " | TTL " + ttl + "s | " + clients + " client(s)",
                jwtByteRange + ". The token travels in every request's Authorization metadata header. " +
                        "Response messages carry no JWT overhead. Validation enforced by the configured gRPC interceptor.", null));

        if (tokenAcqRps > 0) {
            requestsPerSecondModifierTactics.add(tacticsMapperService.tacticEntry("OAuth 2.0 — token acquisition",
                    "1 call per TTL ÷ " + clients + " client(s) — RFC 6749 §4.1",
                    "Periodic call to the authorisation server to obtain a new JWT. " +
                            "Amortised across the token TTL and the number of concurrent clients sharing the token.",
                    "+" + String.format(DISPLAY_LOCALE, "%,d", tokenAcqRps) + " req/s"));
        }

        if (introspectionRps > 0) {
            requestsPerSecondModifierTactics.add(tacticsMapperService.tacticEntry("OAuth 2.0 — remote introspection",
                    "1 introspection call per gRPC request — RFC 7662",
                    "Each incoming gRPC request triggers a synchronous token introspection call to the " +
                            "authorisation server. This doubles outbound request volume. " +
                            "Switching to local JWT signature validation eliminates this overhead entirely.",
                    "+" + String.format(DISPLAY_LOCALE, "%,d", introspectionRps) + " req/s"));
        }
    }

    private void populateMTLS(SecurityTactics securityTactics, List<Map<String, String>> generalTactics, List<Map<String, String>> requestsPerSecondModifierTactics) {
        String mtlsByteRange = TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getFormattedReference();
        long handshakeRps = securityTactics.tlsTactic().extraRequestsPerSecondFromTLSHandshakesAtTheConfiguredReconnectRate();
        String handshakeDetail = String.format(
                "%d reconnects/hr × %d messages (ClientHello + ServerHello/Cert/CertReq + client Cert + CertVerify + Finished)",
                securityTactics.tlsTactic().tlsReconnectsPerHour(), TLSOverhead.MTLS_HANDSHAKE_MESSAGES.getOverhead());
        if (handshakeRps > 0) {
            requestsPerSecondModifierTactics.add(tacticsMapperService.tacticEntry("mTLS — handshake messages",
                    handshakeDetail,
                    "mTLS adds client certificate exchange and CA validation to the TLS handshake. " +
                            mtlsByteRange + ". Per-message overhead is identical to TLS. AWS ACM certificates are free.",
                    "+" + String.format(DISPLAY_LOCALE, "%,d", handshakeRps) + " req/s"));
        } else {
            generalTactics.add(tacticsMapperService.tacticEntry("mTLS (mutual TLS)",
                    securityTactics.tlsTactic().tlsReconnectsPerHour() + " reconnects/hr",
                    "Both client and server present X.509 certificates. CA validates both. " +
                            mtlsByteRange + ". Handshake is connection-scoped — negligible RPS impact at this reconnect rate. AWS ACM certificates are free.",
                    null));
        }
    }

    private void populateTLS(SecurityTactics securityTactics, List<Map<String, String>> generalTactics, List<Map<String, String>> requestsPerSecondModifierTactics) {
        String tlsByteRange = TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getFormattedReference();
        long handshakeRps = securityTactics.tlsTactic().extraRequestsPerSecondFromTLSHandshakesAtTheConfiguredReconnectRate();
        if (handshakeRps > 0) {
            requestsPerSecondModifierTactics.add(tacticsMapperService.tacticEntry("TLS — handshake messages",
                    securityTactics.tlsTactic().tlsReconnectsPerHour() + " reconnects/hr × " + TLSOverhead.TLS_HANDSHAKE_MESSAGES.getOverhead() + " messages",
                    "TLS 1.3 handshake (ClientHello + ServerHello/Certificate/Finished) runs once per connection. " +
                            tlsByteRange + ". AWS ACM issues and renews certificates at no additional cost.",
                    "+" + String.format(DISPLAY_LOCALE, "%,d", handshakeRps) + " req/s"));
        } else {
            generalTactics.add(tacticsMapperService.tacticEntry("TLS (one-way)",
                    securityTactics.tlsTactic().tlsReconnectsPerHour() + " reconnects/hr",
                    "TLS 1.3 encrypts every gRPC message. " + tlsByteRange +
                            ". Handshake is connection-scoped — negligible RPS impact at this reconnect rate. AWS ACM certificates are free.", null));
        }
    }

}
