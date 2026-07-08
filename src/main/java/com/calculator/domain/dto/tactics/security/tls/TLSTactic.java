package com.calculator.domain.dto.tactics.security.tls;

import com.calculator.domain.model.architecture.tactics.security.TLSOverhead;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.calculator.domain.model.architecture.tactics.security.TLSOverhead.MTLS_HANDSHAKE_MESSAGES;
import static com.calculator.domain.model.architecture.tactics.security.TLSOverhead.TLS_HANDSHAKE_MESSAGES;

public record TLSTactic(
        @JsonProperty("tlsEnabled")
        boolean tlsEnabled,
        @JsonProperty("mtlsEnabled")
        boolean mtlsEnabled,
        @JsonProperty("tlsReconnectsPerHour")
        int tlsReconnectsPerHour
) {

    public static TLSTactic empty(){
        return new TLSTactic(false, false,0);
    }

    public int effectiveTlsOverheadTypical()  {
        return (tlsEnabled || mtlsEnabled) ? TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getOverhead() : 0;
    }

    public long extraRequestsPerSecondFromTLSHandshakesAtTheConfiguredReconnectRate() {
        if (tlsIsNotEnabled()) return 0;
        int tlsOverhead = mtlsEnabled ? MTLS_HANDSHAKE_MESSAGES.getOverhead() : TLS_HANDSHAKE_MESSAGES.getOverhead();
        return Math.round(tlsReconnectsPerHour() * (double) tlsOverhead / 3600.0);
    }

    private boolean tlsIsNotEnabled() {
        return !tlsEnabled && !mtlsEnabled;
    }
}
