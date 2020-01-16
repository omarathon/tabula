package uk.ac.warwick.tabula.validators

import javax.validation.{ConstraintValidator, ConstraintValidatorContext}
import org.joda.time.{LocalDate, Years}

class WithinYearsValidatorForLocalDate extends ConstraintValidator[DateWithinYears, LocalDate] {

  var maxPast: Int = _
  var maxFuture: Int = _

  override def initialize(annotation: DateWithinYears): Unit = {
    maxPast = annotation.maxPast()
    maxFuture = annotation.maxFuture()
  }

  override def isValid(v: LocalDate, context: ConstraintValidatorContext): Boolean = {
    val now = LocalDate.now

    Option(v) match {
      case Some(value) if value.isAfter(now) => Years.yearsBetween(now, value).getYears <= maxFuture
      case Some(value) if value.isBefore(now) => Years.yearsBetween(value, now).getYears <= maxPast
      case Some(_) => true // exactly equal to current date
      case _ => true // null values are allowed
    }
  }

}
