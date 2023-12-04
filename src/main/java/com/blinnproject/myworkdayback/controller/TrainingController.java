package com.blinnproject.myworkdayback.controller;

import com.blinnproject.myworkdayback.model.*;
import com.blinnproject.myworkdayback.payload.request.AddExerciseRequest;
import com.blinnproject.myworkdayback.payload.request.ValidateTrainingRequest;
import com.blinnproject.myworkdayback.payload.response.ExerciseWithSeriesResponse;
import com.blinnproject.myworkdayback.repository.SeriesRepository;
import com.blinnproject.myworkdayback.repository.TrainingExercisesRepository;
import com.blinnproject.myworkdayback.service.exercise.ExerciseService;
import com.blinnproject.myworkdayback.service.training.TrainingService;
import com.blinnproject.myworkdayback.security.UserDetailsImpl;
import io.jsonwebtoken.lang.Assert;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/training")
public class TrainingController {

  @Autowired
  TrainingService trainingService;

  @Autowired
  ExerciseService exerciseService;

  @Autowired
  TrainingExercisesRepository trainingExercisesRepository;

  @Autowired
  SeriesRepository seriesRepository;

  @PostMapping()
  public ResponseEntity<Training> create(@Valid @RequestBody Training trainingRequest) {
    try {
      Training _training = trainingService.create(trainingRequest);
      return new ResponseEntity<>(_training, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/current-user")
  public ResponseEntity<List<Training>> getCurrentUserTrainings() {
    UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long createdBy = userDetails.getId();
    try {
      List<Training> trainings = trainingService.getAllTrainingsByCreatedBy(createdBy);

      if (trainings.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      }

      return new ResponseEntity<>(trainings, HttpStatus.OK);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Training> getTrainingById(@PathVariable("id") Long id) {
    Optional<Training> trainingData = trainingService.findById(id);

    if (trainingData.isPresent()) {
      return new ResponseEntity<>(trainingData.get(), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/{trainingId}/add-exercise")
  public ResponseEntity<?> addExercise(@PathVariable("trainingId") Long trainingId, @Valid @RequestBody AddExerciseRequest requestBody) {
    try {
      TrainingExercises trainingExercises = new TrainingExercises();

      trainingExercises.setTraining(trainingService.findById(trainingId).orElse(null));
      trainingExercises.setExercise(exerciseService.findById(requestBody.getExerciseId()).orElse(null));
      trainingExercises.setNotes(requestBody.getNotes());
      trainingExercises.setNumberOfWarmUpSeries(requestBody.getNumberOfWarmUpSeries());

      List<Series> seriesList = requestBody.getSeries();
      trainingExercises.addSeriesList(seriesList);

      TrainingExercises createdTrainingExercises = trainingExercisesRepository.save(trainingExercises);

      return new ResponseEntity<>(createdTrainingExercises, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Failed to save TrainingExercises", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/{trainingId}/exercises")
  public ResponseEntity<List<TrainingExercises>> getExercisesByTrainingId(@PathVariable("trainingId") Long trainingId) {
    List<TrainingExercises> trainingExercises = trainingExercisesRepository.findByTrainingId(trainingId);

    return new ResponseEntity<>(trainingExercises, HttpStatus.OK);
  }

  @PostMapping("/{trainingId}/validate")
  public ResponseEntity<List<TrainingExercises>> validateTraining(@PathVariable("trainingId") Long trainingId, @Valid @RequestBody ValidateTrainingRequest requestBody) throws Exception {

    // Authorization and checkup
    Training training = trainingService.findById(trainingId).orElseThrow(() -> new Exception("Training not found with id " + trainingId));
    UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long createdBy = userDetails.getId();
    Assert.state(Objects.equals(training.getCreatedBy(), createdBy), "Training with id " + trainingId + " does not belong to current user");

    // Check if trainingDay day is included in training trainingDays and if not already set
    Format formatter = new SimpleDateFormat("u");
    DayOfWeek currentDay = DayOfWeek.of(Integer.parseInt(formatter.format(requestBody.getTrainingDay())));
    Assert.state(training.getTrainingDays().contains(currentDay), "The day: " + currentDay + " is not in training days list: " + training.getTrainingDays());

    List<TrainingExercises> trainingExercises = trainingExercisesRepository.findByTrainingId(trainingId);

    List<TrainingExercises> clonedExercises = new ArrayList<>();
    for (TrainingExercises original : trainingExercises) {
      TrainingExercises cloned = new TrainingExercises(original);
      cloned.setTrainingDay(requestBody.getTrainingDay());
      clonedExercises.add(cloned);
    }
    List<TrainingExercises> createdTrainingExercises = trainingExercisesRepository.saveAll(clonedExercises);

    return new ResponseEntity<>(createdTrainingExercises, HttpStatus.OK);
  }

  // todo validate patch

  // todo return all template of TrainingExercises and trainingDay = today if exist
  // or return true if series or exercise are done for each exercise and series
}
