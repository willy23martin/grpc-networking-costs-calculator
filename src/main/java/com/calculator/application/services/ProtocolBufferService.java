package com.calculator.application.services;

import com.calculator.domain.model.protofiles.JavaParsedProtoFile;
import com.calculator.domain.model.protofiles.ParsedProtocolBufferFile;
import com.calculator.domain.model.compilers.ProtocCompiler;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.calculator.shared.ProtocolBufferFileParser.parseProtoFileFrom;
import static com.calculator.shared.ProtocolBuffersUtils.importGoogleProtocolBufferFileDependencies;
import static com.calculator.shared.ProtocolBuffersUtils.resolveProtocolBufferFilesCompiler;

@NoArgsConstructor
@Service
public class ProtocolBufferService {

    private static final String TEMPORARY_PROTO_FILE_PROCESSING_DIRECTORY = "tempParserDirectory";

    public ParsedProtocolBufferFile parse(MultipartFile protoFile) throws IOException {
        Path protocolBufferFileDirectory = Files.createTempDirectory(TEMPORARY_PROTO_FILE_PROCESSING_DIRECTORY);
        Path protoPath = protocolBufferFileDirectory.resolve(protoFile.getOriginalFilename());
        protoFile.transferTo(protoPath);

        JavaParsedProtoFile javaParsedProtoFile = parseProtoFileFrom(protoPath);
        ParsedProtocolBufferFile parsedProtocolBufferFile = new ParsedProtocolBufferFile(protocolBufferFileDirectory, protoPath, javaParsedProtoFile);
        return parsedProtocolBufferFile;
    }

    public ProtocCompiler runProtocOver(ParsedProtocolBufferFile parsedProtocolBufferFile) throws IOException {
        importGoogleProtocolBufferFileDependencies(parsedProtocolBufferFile.protocolBufferFileDirectory());
        Path javaOutDir = parsedProtocolBufferFile.protocolBufferFileDirectory().resolve("java");
        Files.createDirectories(javaOutDir);
        Process protoc = new ProcessBuilder(
                resolveProtocolBufferFilesCompiler().toAbsolutePath().toString(),
                "--java_out=" + javaOutDir.toAbsolutePath(),
                "-I" + parsedProtocolBufferFile.protocolBufferFileDirectory().toAbsolutePath(),   // Because single -I covers both proto and well-known types
                parsedProtocolBufferFile.protoPath().toAbsolutePath().toString()
        ).redirectErrorStream(true).start();
        ProtocCompiler protoCompiler = new ProtocCompiler(javaOutDir, protoc);
        return protoCompiler;
    }

}
