package com.imbilalbutt.patientservice.service;

import com.imbilalbutt.patientservice.dto.PatientRequestDTO;
import com.imbilalbutt.patientservice.dto.PatientResponseDTO;
import com.imbilalbutt.patientservice.exceptions.EmailAlreadyExistsExceptions;
import com.imbilalbutt.patientservice.exceptions.PatientNotFoundException;
import com.imbilalbutt.patientservice.mapper.PatientMapper;
import com.imbilalbutt.patientservice.model.Patient;
import com.imbilalbutt.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

//   Pattern: Dependecny injection
//    PatientService recerives its dependency (patientRepository) by its constructor, instead of instantitating
//    the patientRepository itself by using new() keyword.
    private PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients(){
        List<Patient> patients = patientRepository.findAll();
        List<PatientResponseDTO> dtos = patients.stream()
//                .map(patient -> PatientMapper.toDTO(patient))
                .map(PatientMapper::toDTO)
                .toList();

        return dtos;
    }


    public PatientResponseDTO createPatient(PatientRequestDTO requestDTO){

        if (patientRepository.existsByEmail(requestDTO.getEmail())){
//            Patient newPatient = patientRepository.save(
//                    PatientMapper.toModel(requestDTO));
            throw new EmailAlreadyExistsExceptions("A patient with this Email" + requestDTO.getEmail() + " already exists.");
        }
        Patient newPatient = patientRepository.save(
                PatientMapper.toModel(requestDTO));

        return PatientMapper.toDTO(newPatient);
    }


    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO requestDTO){
        Patient patient = patientRepository.findById(id).orElseThrow(
                () -> new PatientNotFoundException("Patient not found with ID: " + id));

        if (patientRepository.existsByEmailAndIdNot(requestDTO.getEmail(), id)){
            throw new EmailAlreadyExistsExceptions("A patient with this Email" + requestDTO.getEmail() + " already exists.");
        }

        patient.setName(requestDTO.getName());
        patient.setAddress(requestDTO.getAddress());
        patient.setEmail(requestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(requestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);

        return PatientMapper.toDTO(updatedPatient);
    }


    public void deletePatient(UUID id){
        patientRepository.deleteById(id);
    }

}
