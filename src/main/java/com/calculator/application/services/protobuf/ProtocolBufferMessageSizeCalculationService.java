package com.calculator.application.services.protobuf;

import com.calculator.domain.dto.results.MessageSizeCalculationResult;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLClassLoader;
import java.util.logging.Logger;

import static com.calculator.shared.ProtocolBuffersUtils.populateFieldsForRepresentativeSize;

@Service
public class ProtocolBufferMessageSizeCalculationService {

    @Value("${protofile.max.repeated.items}")
    int globalMaxRepeatedItems;

    private static final java.util.logging.Logger log = Logger.getLogger(ProtocolBufferMessageSizeCalculationService.class.getName());

    public MessageSizeCalculationResult getMessageSize(URLClassLoader classLoader, String fullClassNameForRequestMessage) throws Exception {
        Class<?> messageClass = classLoader.loadClass(fullClassNameForRequestMessage); // Reflection
        log.info("Request Class loaded: " + messageClass.getName());

        Descriptors.Descriptor requestDescriptor = (Descriptors.Descriptor) messageClass.getMethod("getDescriptor").invoke(null);
        DynamicMessage.Builder requestBuilder = DynamicMessage.newBuilder(requestDescriptor);

        populateFieldsForRepresentativeSize(requestBuilder, requestDescriptor, globalMaxRepeatedItems);

        DynamicMessage messageInstance = requestBuilder.build();
        return new MessageSizeCalculationResult(messageClass, messageInstance.toByteArray().length);
    }

}
