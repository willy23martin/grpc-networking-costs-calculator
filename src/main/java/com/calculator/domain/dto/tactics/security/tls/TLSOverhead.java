package com.calculator.domain.dto.tactics.security.tls;

public enum TLSOverhead {

    RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX(
            30,
            """
                    Max Payload overhead: bytes per message — RFC 8446\n
                    Because of RFC 8446 Section-5.2 \n
                         * TLSCiphertext outer header: 5 bytes: \n
                         *    + 1-byte opaque_type=23 (uint8 ContentType) \n
                         *    + 2-byte legacy_record_version (uint16 ProtocolVersion) \n
                         *    + 2-byte length (uint16 length).\n
                         * TLSInnerPlaintext prefix (AAD):\n
                         *    + 1-byte Content type and no padding.\n
                         * AEAD Pre-record nonce: RFC 8446 Section-5.3\n
                         *    + 8-byte length (64 bits explicit portion added during encryption).\n
                         * AEAD tag: RFC 5116 Section-5.1\n
                         *    + 16-byte tag appended to encrypted_record for\n
                         AEAD_AES_128_GCM ciphertext is exactly 16 octets longer than its corresponding plaintext\n
                    """
            ),
    TLS_HANDSHAKE_MESSAGES(2,
            """
            Requests because of the TLS handshake messages per reconnect \n (connection-scoped, not per-request):\n
            \n ClientHello + ServerHello+Cert+Finished\n
            """),
    MTLS_HANDSHAKE_MESSAGES(5, """ 
    requests as adds \n CertificateRequest + client Cert + CertificateVerify\n
    """);

    private final int overhead;
    private final String reference;

    TLSOverhead(int overhead, String reference){
        this.overhead = overhead;
        this.reference = reference;
    }

    public int getOverhead() {
        return this.overhead;
    }

    public String getFormattedReference(){
        return  String.format("%d: %s",
                this.overhead,
                this.reference
        );
    }

}
