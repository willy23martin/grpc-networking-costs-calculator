package com.calculator.domain.dto.tactics.security.oauth.jwt;

public enum JWTOverhead {

    /**
     *Because of the RFC 7519 §3, and https://www.rfc-editor.org/rfc/rfc7515.html#section-7.1 base64url-encoded header.payload.signature - JWT Bearer token size
     * Total JWT Bytes = len(base64url(header)) + 2 + len(base64url(payload)) + 2 + len(base64url(signature))
     * 1. Header_bytes = ceil(4/3 * UTF8_len('{"typ":"JWT","alg":"ES256"}'))  ≈ 60-80 bytes
     *    (Fixed: typ+alg; varies slightly by whitespace/alg string length)
     *
     * 2. Payload_bytes = ceil(4/3 * UTF8_len(claims_JSON))
     *    Minimal RFC example: ceil(4/3 * 83) = 112 bytes
     *    Compact claims: 100-200 UTF8 → 140-270 bytes
     *    Standard claims (iss/sub/aud/exp/iat): 300-500 UTF8 → 400-670 bytes
     *
     * 3. Signature_bytes = ceil(4/3 * raw_signature_length_bytes)
     *    - ES256 (ECDSA P-256): raw DER ≈ 70 bytes → 96 bytes encoded [RFC 7515 §3.4]
     *    - RS256 (RSA-2048): raw PKCS#1 ≈ 256 bytes → 344 bytes [RFC 8017]
     *    - RS512 (RSA-4096): raw PKCS#1 ≈ 512 bytes → 688 bytes
     */

    JWT_OVERHEAD_BYTES_MIN(300, """
            \n
            | Algorithm | Compact claims ES256 | \n
            | Header |  72  | \n
            Payload (compact/std/ext) |  130/200/300 | \n
            Signature | 96 | \n
            Dots | 2 | \n
            Total   300/370/470 bytes |\n
            """),
    JWT_OVERHEAD_BYTES_TYPICAL(650, """
            \n
            | Algorithm | Standard claims RS256 \n
            | Header | 80 \n
            | Payload (compact/std/ext) | 200/400/600 \n
            | Signature | 344 \n
            | Dots | 2 \n
            | Total | 626/826/1026 → 650 typical \n
            """),
    JWT_OVERHEAD_BYTES_MAX(1200, """
            \n
            | Algorithm | RS512 |\n
            | Header | 84 |\n
            | Payload (compact/std/ext) | 300/500/800 |\n
            | Signature | 688 |\n
            | Dots | 2 |\n
            | Total | 974/1174/1474 → 1200 max  |\n
            """);

    private final int overhead;
    private final String reference;

    JWTOverhead(int overhead, String reference){
        this.overhead = overhead;
        this.reference = reference;
    }

    public int getOverhead() {
        return this.overhead;
    }

    public String getFormattedReference(){
        return  String.format("Payload overhead \nRFC 7519 §3 and RFC 9101: \n%d bytes per message:\n %s",
                this.overhead,
                this.reference
        );
    }

}
