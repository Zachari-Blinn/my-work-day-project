package com.blinnproject.myworkdayback.controller;

import com.blinnproject.myworkdayback.model.Training;
import com.blinnproject.myworkdayback.model.TrainingExercises;
import com.blinnproject.myworkdayback.payload.dto.training.TrainingCreateDTO;
import com.blinnproject.myworkdayback.payload.dto.training_exercises.TrainingExercisesCreateDTO;
import com.blinnproject.myworkdayback.payload.query.TrainingCalendarDTO;
import com.blinnproject.myworkdayback.payload.request.ModifyAndValidateRequest;
import com.blinnproject.myworkdayback.payload.response.FormattedTrainingData;
import com.blinnproject.myworkdayback.payload.response.GenericResponse;
import com.blinnproject.myworkdayback.payload.response.TrainingExercisesSeriesInfo;
import com.blinnproject.myworkdayback.security.UserDetailsImpl;
import com.blinnproject.myworkdayback.service.training.TrainingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/training")
public class TrainingController {

  private final TrainingService trainingService;

  public TrainingController(TrainingService trainingService) {
    this.trainingService = trainingService;
  }

  @PostMapping()
  public ResponseEntity<GenericResponse<Training>> create(@Valid @RequestBody TrainingCreateDTO trainingDTO) {
    Training training = trainingService.create(trainingDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(GenericResponse.success(training, "Training was successfully created!"));
  }

  @Deprecated
  @GetMapping("/current-user")
  public ResponseEntity<GenericResponse<List<Training>>> getCurrentUserTrainings() {
    UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<Training> trainings = trainingService.getAllTrainingsByCreatedBy(userDetails.getId());

    if (trainings.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    return ResponseEntity.ok(GenericResponse.success(trainings, "Return all trainings of current user successfully!"));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GenericResponse<Training>> getTrainingById(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
    Optional<Training> trainingData = trainingService.findById(id, userDetails.getId());

    return trainingData.map(training -> ResponseEntity.ok(GenericResponse.success(training, "Return training by id successfully!"))).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping("/{trainingId}/exercise/{exerciseId}")
  public ResponseEntity<GenericResponse<TrainingExercises>> addExerciseToTraining(
      @PathVariable("trainingId") Long trainingId,
      @PathVariable("exerciseId") Long exerciseId,
      @Valid @RequestBody TrainingExercisesCreateDTO trainingExercisesDTO,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    TrainingExercises createdTrainingExercises = this.trainingService.addExercise(trainingId, exerciseId, trainingExercisesDTO, userDetails.getId());

    return ResponseEntity.ok(GenericResponse.success(createdTrainingExercises, "Exercise added to training successfully!"));
  }

  @Deprecated
  @GetMapping("/{trainingId}/exercises")
  public ResponseEntity<GenericResponse<List<TrainingExercises>>> getExercisesByTrainingId(
      @RequestParam(defaultValue = "false") Boolean fetchTemplate,
      @PathVariable("trainingId") Long trainingId,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    List<TrainingExercises> trainingExercises;

    if (fetchTemplate) {
      trainingExercises = this.trainingService.getTemplateExercisesByTrainingId(trainingId, userDetails.getId());
    } else {
      trainingExercises = this.trainingService.getExercisesByTrainingId(trainingId, userDetails.getId());
    }

    return ResponseEntity.ok(GenericResponse.success(trainingExercises, "Return all exercises by training successfully!"));
  }

  @PostMapping("/{trainingParentId}/validate/{trainingDate}")
  public ResponseEntity<GenericResponse<List<TrainingExercises>>> validateTrainingSession(
      @PathVariable("trainingParentId") Long trainingParentId,
      @PathVariable("trainingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date trainingDate,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    List<TrainingExercises> createdTrainingExercises = this.trainingService.validateTraining(trainingParentId, trainingDate, userDetails.getId());

    return ResponseEntity.ok(GenericResponse.success(createdTrainingExercises, "Validate training session successfully!"));
  }

  @PostMapping("/{trainingParentId}/modify-and-validate/{trainingDate}")
  public ResponseEntity<GenericResponse<List<TrainingExercises>>> modifyAndValidateTrainingSession(
      @PathVariable("trainingParentId") Long trainingParentId,
      @PathVariable("trainingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date trainingDate,
      @RequestBody ModifyAndValidateRequest requestBody,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    List<TrainingExercises> createdTrainingExercises = this.trainingService.modifyAndValidateTraining(trainingParentId, trainingDate, requestBody, userDetails.getId());

    return ResponseEntity.ok(GenericResponse.success(createdTrainingExercises, "Modify and validate training session successfully!"));
  }

  @Deprecated
  @GetMapping("/{trainingId}/validate/{trainingDate}")
  public ResponseEntity<GenericResponse<List<FormattedTrainingData>>> returnTrainingSessionInfo(
      @PathVariable("trainingId") Long trainingId,
      @PathVariable("trainingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date trainingDate,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    List<TrainingExercisesSeriesInfo> trainingExercisesSeriesInfoList = this.trainingService.getTrainingSeriesStatusByDate(trainingId, trainingDate, userDetails.getId());

    List<FormattedTrainingData> transformedData = this.trainingService.formatTrainingExercisesSeriesInfo(trainingExercisesSeriesInfoList, trainingDate);

    return ResponseEntity.ok(GenericResponse.success(transformedData, "Return training session info of selected day successfully!"));
  }

  @GetMapping("/validate/{trainingDate}")
  public ResponseEntity<GenericResponse<List<FormattedTrainingData>>> returnAllTrainingSessionInfo(
      @PathVariable("trainingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date trainingDate,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    List<TrainingExercisesSeriesInfo> trainingExercisesSeriesInfoList = this.trainingService.getAllTrainingsSeriesStatusByDate(trainingDate, userDetails.getId());

    List<FormattedTrainingData> transformedData = this.trainingService.formatTrainingExercisesSeriesInfo(trainingExercisesSeriesInfoList, trainingDate);

    return ResponseEntity.ok(GenericResponse.success(transformedData, "Return training session info of selected day successfully!"));
  }

  @DeleteMapping("/{trainingParentId}/reset/{trainingDate}")
  public ResponseEntity<GenericResponse<?>> resetTrainingSession(
      @PathVariable("trainingParentId") Long trainingParentId,
      @PathVariable("trainingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date trainingDate,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    this.trainingService.resetTrainingDay(trainingParentId, trainingDate, userDetails.getId());

    return ResponseEntity.ok(GenericResponse.success(null, "Cancelled training session info of selected day successfully!"));
  }

  @DeleteMapping("/{trainingParentId}/cancel/{trainingDate}")
  public ResponseEntity<GenericResponse<?>> cancelTrainingSession(
      @PathVariable("trainingParentId") Long trainingParentId,
      @PathVariable("trainingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date trainingDate,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    this.trainingService.cancelTrainingDay(trainingParentId, trainingDate, userDetails.getId());

    return ResponseEntity.ok(GenericResponse.success(null, "Cancelled training session info of selected day successfully!"));
  }

  @GetMapping("/calendar/{startDate}/{endDate}")
  public ResponseEntity<GenericResponse<List<TrainingCalendarDTO>>> returnTrainingCalendarInfo(
      @PathVariable("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
      @PathVariable("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) throws Exception {
    List<TrainingCalendarDTO> result = this.trainingService.getTrainingCalendarInfo(startDate, endDate, userDetails.getId());

    return ResponseEntity.ok(GenericResponse.success(result, "Return calendar info successfully!"));
  }
}
