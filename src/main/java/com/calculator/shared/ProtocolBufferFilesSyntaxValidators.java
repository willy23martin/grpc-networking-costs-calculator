package com.calculator.shared;

import java.util.regex.Pattern;

public class ProtocolBufferFilesSyntaxValidators {

    // Validates optional "stream" keyword before request/response types for client, server and bi-directional streaming proto file definitions
    public static final Pattern rpcMethodPattern = Pattern.compile(
            "^\\s*rpc\\s+[a-zA-Z0-9_]+\\s*\\(\\s*(?:stream\\s+)?([a-zA-Z0-9_.]+)\\s*\\)\\s*returns\\s*\\(\\s*(?:stream\\s+)?([a-zA-Z0-9_.]+)\\s*\\)\\s*[;{]"
    );

    // Validates optional java_outer_classname in proto3
    public static final Pattern javaOuterClassnamePattern = Pattern.compile(
            "^\\s*option\\s+java_outer_classname\\s*=\\s*\"([a-zA-Z0-9_]+)\"\\s*;"
    );

    // Validates optional java_package and also proto3 dots and underscores
    public static final Pattern javaPackageOptionPattern = Pattern.compile(
            "^\\s*option\\s+java_package\\s*=\\s*\"([a-zA-Z0-9_.]+)\"\\s*;"
    );

}
