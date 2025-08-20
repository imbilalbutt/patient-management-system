package com.imbilalbutt.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    // 1. Specify the topic that we want to listen to or in other words the topic that we want to consume events from
    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void ConsumeEvent(byte[] event){

        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // .. perform any business logic related to analytics here
//            any logic by calling serive class or repository

            log.info("Received Patient Event: [PatientId = {}, PatientName = {}, patientEmail = {}] ", patientEvent.getPatientId(),
                    patientEvent.getName(), patientEvent.getEmail());
        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserizalizing event : {}", e.getMessage());
//            throw new RuntimeException(e);
        }

    }
}
