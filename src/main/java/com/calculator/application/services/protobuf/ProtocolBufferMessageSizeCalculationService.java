package com.calculator.application.services.protobuf;

import com.calculator.domain.dto.results.MessageSizeCalculationResult;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLClassLoader;

import static com.calculator.shared.ProtocolBuffersUtils.populateFieldsForMaxSize;

@Service
public class ProtocolBufferMessageSizeCalculationService {

    @Value("${protofile.max.repeated.items}")
    int globalMaxRepeatedItems;

    public MessageSizeCalculationResult getMessageSize(URLClassLoader classLoader, String fullClassNameForRequestMessage) throws Exception {
        Class<?> messageClass = classLoader.loadClass(fullClassNameForRequestMessage);
        System.out.println("Request Class loaded: " + messageClass.getName());

        Descriptors.Descriptor requestDescriptor = (Descriptors.Descriptor) messageClass.getMethod("getDescriptor").invoke(null);
        DynamicMessage.Builder requestBuilder = DynamicMessage.newBuilder(requestDescriptor);

        populateFieldsForMaxSize(requestBuilder, requestDescriptor, globalMaxRepeatedItems);

        DynamicMessage messageInstance = requestBuilder.build();
        return new MessageSizeCalculationResult(messageClass, messageInstance.toByteArray().length);
    }

}
