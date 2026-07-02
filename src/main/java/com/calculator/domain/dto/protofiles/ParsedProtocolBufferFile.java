package com.calculator.domain.dto.protofiles;

import java.nio.file.Path;

public record ParsedProtocolBufferFile(
        Path protocolBufferFileDirectory,
        Path protoPath,
        JavaParsedProtoFile javaParsedProtoFile) {
}