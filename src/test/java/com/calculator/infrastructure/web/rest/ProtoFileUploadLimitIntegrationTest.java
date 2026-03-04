package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.calculator.infrastructure.web.rest.TCOCalculatorControllerTest.VALID_PROTO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtoFileUploadLimitIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldNotThrowFileCountLimitExceededExceptionWhenManyFilesUploaded() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (int i = 0; i < 199; i++) {
            final String filename = "order_" + i + ".proto";
            body.add("protoFile", new ByteArrayResource(VALID_PROTO_CONTENT.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
        }

        body.add("requestsPerSecond", "1000");
        body.add("tacticClientLb", "true");
        body.add("tacticTimeout", "true");
        body.add("tacticTimeoutMs", "300");
        body.add("tacticRetry", "true");
        body.add("tacticRetryTimes", "3");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/calculateTCO", requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}