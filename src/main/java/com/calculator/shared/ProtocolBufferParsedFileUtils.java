package com.calculator.shared;

import com.calculator.domain.model.protofiles.JavaParsedProtoFile;
import com.calculator.domain.model.properties.ProtoFileFullyQualifiedProperties;

public class ProtocolBufferParsedFileUtils {

    public static boolean isValid(JavaParsedProtoFile javaParsedProtoFile) {
        return !javaParsedProtoFile.requestMessageSimpleName().isEmpty() && !javaParsedProtoFile.responseMessageSimpleName().isEmpty();
    }

    public static ProtoFileFullyQualifiedProperties initializeProtoFileFullyQualifiedProperties(JavaParsedProtoFile javaParsedProtoFile) {
        String fullRequestMessageClassName = extractFullRequestMessageClassName(javaParsedProtoFile);

        String fullResponseMessageClassName = extractFullResponseMessageClassName(javaParsedProtoFile);
        return new ProtoFileFullyQualifiedProperties(fullRequestMessageClassName, fullResponseMessageClassName);
    }

    private static String extractFullResponseMessageClassName(JavaParsedProtoFile javaParsedProtoFile) {
        String fullResponseMessageClassName;
        if (!javaParsedProtoFile.javaPackageName().isEmpty()) {
            if (!javaParsedProtoFile.outerClassName().isEmpty()) {
                fullResponseMessageClassName = javaParsedProtoFile.javaPackageName() + "." + javaParsedProtoFile.outerClassName() + "$" + javaParsedProtoFile.responseMessageSimpleName();
            } else {
                fullResponseMessageClassName = javaParsedProtoFile.javaPackageName() + "." + javaParsedProtoFile.responseMessageSimpleName();
            }
        } else {
            if (!javaParsedProtoFile.outerClassName().isEmpty()) {
                fullResponseMessageClassName = javaParsedProtoFile.outerClassName() + "$" + javaParsedProtoFile.responseMessageSimpleName();
            } else {
                fullResponseMessageClassName = javaParsedProtoFile.responseMessageSimpleName();
            }
        }
        return fullResponseMessageClassName;
    }

    private static String extractFullRequestMessageClassName(JavaParsedProtoFile javaParsedProtoFile) {
        String fullRequestMessageClassName;
        if (!javaParsedProtoFile.javaPackageName().isEmpty()) {
            if (!javaParsedProtoFile.outerClassName().isEmpty()) {
                fullRequestMessageClassName = javaParsedProtoFile.javaPackageName() + "." + javaParsedProtoFile.outerClassName() + "$" + javaParsedProtoFile.requestMessageSimpleName();
            } else {
                fullRequestMessageClassName = javaParsedProtoFile.javaPackageName() + "." + javaParsedProtoFile.requestMessageSimpleName();
            }
        } else {
            if (!javaParsedProtoFile.outerClassName().isEmpty()) {
                fullRequestMessageClassName = javaParsedProtoFile.outerClassName() + "$" + javaParsedProtoFile.requestMessageSimpleName();
            } else {
                fullRequestMessageClassName = javaParsedProtoFile.requestMessageSimpleName();
            }
        }
        return fullRequestMessageClassName;
    }

}
