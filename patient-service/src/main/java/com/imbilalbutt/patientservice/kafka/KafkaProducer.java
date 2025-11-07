package com.imbilalbutt.patientservice.kafka;


import com.imbilalbutt.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

// patient.events are coming from target/generated-sources/protobuf/java/patient.events
import patient.events.PatientEvent;

@Service
// This code here will create a kafka producer
// This class would be responsible for sending events to a given kafka topic
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

//    1st thing we need is called kafka template

//    This is how we define message types and we use kafkaTemplate to actually send those messages as well
//    So we are specifying that our messages that we send to a kafka topic from this producer are going to be
//    of key-value pair
//    This is telling kafka that we are going to be sending kafka event that has key of type String and value as byte array
//    So anytime we produce and send a message we're going to convert all the data we want to send into byte array and we
//    are going to add that to event and will also add key to event and we will send that event using kafka event
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient){
//        1. Create an event that has all the properties
        PatientEvent patientEvent = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        try {
//            2. Send a message
            kafkaTemplate.send("patient", patientEvent.toByteArray());

        } catch (Exception e){
            log.error("Error sending message PatientCreated even : {} " , e.getMessage());
        }


    }
}
