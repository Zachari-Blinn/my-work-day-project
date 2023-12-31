package com.blinnproject.myworkdayback.payload.dto.training_exercises;

import com.blinnproject.myworkdayback.model.Exercise;
import com.blinnproject.myworkdayback.model.Series;
import com.blinnproject.myworkdayback.model.Training;
import com.blinnproject.myworkdayback.model.TrainingExercises;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TrainingExercisesDTO {
  @Id
  @NotNull
  private Long id;

  private TrainingExercises parent;

  @NotNull
  private Training training;

  @NotNull
  private Exercise exercise;

  private List<Series> seriesList = new ArrayList<>();

  private String notes;

  @Min(0)
  private int numberOfWarmUpSeries;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private Date trainingDay;
}
