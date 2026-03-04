package com.calculator.shared;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ProtocolBuffersUtils {

    private static final String PROTOC_DEPENDENCY_VERSION = "4.29.4";

    private static final String GOOGLE_PROTOCOL_BUFFERS_WELL_KNOWN_TYPES_PREFIX = "google/protobuf";

    public static final String USER_HOME = "user.home";
    public static final String MAVEN_REPOSITORY = "/.m2/repository";
    public static final String OS_NAME = "os.name";
    public static final String OS_ARCHITECTURE = "os.arch";
    public static final String OS_WINDOWS_INDICATOR = "win";
    public static final String WINDOWS_X_86_64 = "windows-x86_64";
    public static final String OS_MAC_INDICATOR = "mac";
    public static final String OS_MAC_AARCH_64_INDICATOR = "aarch64";
    public static final String OSX_MAC_AARCH_64_INDICATOR = "osx-aarch_64";
    public static final String OSX_MAC_X_86_64_INDICATOR = "osx-x86_64";
    public static final String OS_LINUX_AARCH_64_INDICATOR = "aarch64";
    public static final String LINUX_AARCH_64_INDICATOR = "linux-aarch_64";
    public static final String LINUX_X_86_64_INDICATOR = "linux-x86_64";
    public static final String WINDOWS_DOT_EXE_EXTENSION = ".exe";
    public static final int ARBITRARY_LARGE_STRING_LENGTH = 255;

    public static Path importGoogleProtocolBufferFileDependencies(Path protocolBufferFileDirectory) throws IOException {
        Path googleProtobufDirectory = protocolBufferFileDirectory.resolve(GOOGLE_PROTOCOL_BUFFERS_WELL_KNOWN_TYPES_PREFIX);
        Files.createDirectories(googleProtobufDirectory);

        List.of("timestamp.proto", "duration.proto", "wrappers.proto", "any.proto").stream()
                .forEach(
                        googleProtobufWellKnownType ->  {
                            try {
                                resolve(googleProtobufWellKnownType, googleProtobufDirectory);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );

        return protocolBufferFileDirectory;
    }

    private static void resolve(String googleProtobufWellKnownType, Path googleProtobufDirectory) throws IOException {
        try (InputStream googleProtobufWellKnownTypeStream = ProtocolBuffersUtils.class.getResourceAsStream("/"+ GOOGLE_PROTOCOL_BUFFERS_WELL_KNOWN_TYPES_PREFIX +"/" + googleProtobufWellKnownType)) {
            if (googleProtobufWellKnownTypeStream != null) {
                Files.copy(googleProtobufWellKnownTypeStream, googleProtobufDirectory.resolve(googleProtobufWellKnownType), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static Path resolveProtocolBufferFilesCompiler() {
        String mavenRepositoryDirectory = System.getProperty(USER_HOME) + MAVEN_REPOSITORY;
        String operativeSystem = System.getProperty(OS_NAME).toLowerCase();
        String operativeSystemArchitecture = System.getProperty(OS_ARCHITECTURE).toLowerCase();

        String classifier = getClassifier(operativeSystem, operativeSystemArchitecture);

        Path protocPath = getProtocPath(classifier, operativeSystem, mavenRepositoryDirectory);

        return protocPath;
    }

    private static Path getProtocPath(String classifier, String operativeSystem, String mavenRepositoryDirectory) {
        String filename = "protoc-" + PROTOC_DEPENDENCY_VERSION + "-" + classifier +
                (operativeSystem.contains(OS_WINDOWS_INDICATOR) ? WINDOWS_DOT_EXE_EXTENSION : "");

        Path protocPath = Path.of(mavenRepositoryDirectory,
                "com/" + GOOGLE_PROTOCOL_BUFFERS_WELL_KNOWN_TYPES_PREFIX + "/protoc",
                PROTOC_DEPENDENCY_VERSION, filename);

        if (!operativeSystem.contains(OS_WINDOWS_INDICATOR)) {
            protocPath.toFile().setExecutable(true);
        }
        return protocPath;
    }

    private static String getClassifier(String operativeSystem, String operativeSystemArchitecture) {
        if (operativeSystem.contains(OS_WINDOWS_INDICATOR)) {
            return WINDOWS_X_86_64;
        } else if (operativeSystem.contains(OS_MAC_INDICATOR)) {
            return operativeSystemArchitecture.contains(OS_MAC_AARCH_64_INDICATOR) ? OSX_MAC_AARCH_64_INDICATOR : OSX_MAC_X_86_64_INDICATOR;
        } else {
            return operativeSystemArchitecture.contains(OS_LINUX_AARCH_64_INDICATOR) ? LINUX_AARCH_64_INDICATOR : LINUX_X_86_64_INDICATOR;
        }
    }

    public static void populateFieldsForMaxSize(DynamicMessage.Builder dynamicMessageBuilder, Descriptors.Descriptor messageDescriptor, int maxRepeatedItems) throws Exception {
        for (Descriptors.FieldDescriptor fieldDescriptor : messageDescriptor.getFields()) {
            if (fieldDescriptor.isRepeated()) {
                populateRepeated(dynamicMessageBuilder, maxRepeatedItems, fieldDescriptor);
            } else {
                populateSingular(dynamicMessageBuilder, maxRepeatedItems, fieldDescriptor);
            }
        }
    }

    private static void populateSingular(DynamicMessage.Builder dynamicMessageBuilder, int maxRepeatedItems, Descriptors.FieldDescriptor fieldDescriptor) throws Exception{
        if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
            recursivelyPopulateSingularNestedMessage(dynamicMessageBuilder, maxRepeatedItems, fieldDescriptor);
        } else if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
            populateSingularEnum(dynamicMessageBuilder, fieldDescriptor);
        } else {
            Object value = getMaxValue(fieldDescriptor.getJavaType(), null); // Because is not an enum type
            dynamicMessageBuilder.setField(fieldDescriptor, value);
        }
    }

    private static void populateSingularEnum(DynamicMessage.Builder dynamicMessageBuilder, Descriptors.FieldDescriptor fieldDescriptor) {
        if (!fieldDescriptor.getEnumType().getValues().isEmpty()) {
            dynamicMessageBuilder.setField(fieldDescriptor, fieldDescriptor.getEnumType().getValues().get(fieldDescriptor.getEnumType().getValues().size() - 1));
        } else {
            throw new RuntimeException("Error: Enum fieldDescriptor " + fieldDescriptor.getFullName() + " has no values defined.");
        }
    }

    private static void recursivelyPopulateSingularNestedMessage(DynamicMessage.Builder dynamicMessageBuilder, int maxRepeatedItems, Descriptors.FieldDescriptor fieldDescriptor) throws Exception {
        DynamicMessage.Builder nestedBuilder = DynamicMessage.newBuilder(fieldDescriptor.getMessageType());
        populateFieldsForMaxSize(nestedBuilder, fieldDescriptor.getMessageType(), maxRepeatedItems);
        dynamicMessageBuilder.setField(fieldDescriptor, nestedBuilder.build());
    }

    private static void populateRepeated(DynamicMessage.Builder dynamicMessageBuilder, int maxRepeatedItems, Descriptors.FieldDescriptor fieldDescriptor) throws Exception {
        for (int i = 0; i < maxRepeatedItems; i++) {
            if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                recursivelyPopulateNestedRepeatedMessage(dynamicMessageBuilder, maxRepeatedItems, fieldDescriptor);
            } else {
                populateRepeatedEnumTypes(dynamicMessageBuilder, fieldDescriptor);
            }
        }
    }

    private static void populateRepeatedEnumTypes(DynamicMessage.Builder builder, Descriptors.FieldDescriptor field) {
        Object value = getMaxValue(field.getJavaType(),
                field.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM ? field.getEnumType() : null);
        builder.addRepeatedField(field, value);
    }

    private static void recursivelyPopulateNestedRepeatedMessage(DynamicMessage.Builder builder, int maxRepeatedItems, Descriptors.FieldDescriptor field) throws Exception {
        DynamicMessage.Builder nestedBuilder = DynamicMessage.newBuilder(field.getMessageType());
        populateFieldsForMaxSize(nestedBuilder, field.getMessageType(), maxRepeatedItems);
        builder.addRepeatedField(field, nestedBuilder.build());
    }

    private static Object getMaxValue(Descriptors.FieldDescriptor.JavaType javaType, Descriptors.EnumDescriptor enumType) {
        Object result = null;
        switch (javaType) {
            case INT -> result = Integer.MAX_VALUE;
            case LONG -> result = Long.MAX_VALUE;
            case FLOAT -> result =Float.MAX_VALUE;
            case DOUBLE -> result = Double.MAX_VALUE;
            case BOOLEAN -> result = true;
            case STRING -> result =generateLargeString(ARBITRARY_LARGE_STRING_LENGTH);
            case BYTE_STRING -> result = ByteString.copyFromUtf8(generateLargeString(ARBITRARY_LARGE_STRING_LENGTH));
            case ENUM -> {
                result = getMaxValue(enumType);
            }
            case MESSAGE -> {}
            default -> {
                throw new RuntimeException("Error: Unsupported JavaType for max value generation: " + javaType);
            }
        }
        return result;
    }

    private static Object getMaxValue(Descriptors.EnumDescriptor enumType) {
        if (enumType != null && !enumType.getValues().isEmpty()) {
            return enumType.getValues().get(enumType.getValues().size() - 1);
        } else {
            return 0;
        }
    }

    private static String generateLargeString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('X'); // Because a consistent byte representation is needed.
        }
        return sb.toString();
    }

}
