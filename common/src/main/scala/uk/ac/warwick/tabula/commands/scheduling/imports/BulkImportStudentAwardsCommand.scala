package uk.ac.warwick.tabula.commands.scheduling.imports

import org.springframework.beans.{BeanWrapper, PropertyAccessorFactory}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.imports.BulkImportStudentAwardsCommand._
import uk.ac.warwick.tabula.data.model.{Classification, StudentAward}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling.{AutowiringStudentAwardImporterComponent, StudentAwardImporterComponent, StudentAwardRow}

object BulkImportStudentAwardsCommand {

  case class Result(created: Int, updated: Int, deleted: Int)

  type Command = Appliable[Result]

  def apply(year: AcademicYear): Command =
    new BulkImportStudentAwardsCommandInternal
      with ComposableCommand[Result]
      with ImportSystemDataPermissions
      with PropertyCopying
      with AutowiringStudentAwardImporterComponent
      with AutowiringStudentAwardServiceComponent
      with AutowiringAwardServiceComponent
      with AutowiringClassificationServiceComponent
      with AutowiringTransactionalComponent
      with BulkImportStudentAwardsForAcademicYearDescription
      with BulkImportStudentAwardsForAcademicYearRequest {
      override val academicYear: AcademicYear = year
    }

  def apply(ids: Seq[String]): Command =
    new BulkImportStudentAwardsCommandInternal
      with ComposableCommand[Result]
      with ImportSystemDataPermissions
      with PropertyCopying
      with AutowiringStudentAwardImporterComponent
      with AutowiringStudentAwardServiceComponent
      with AutowiringAwardServiceComponent
      with AutowiringClassificationServiceComponent
      with AutowiringTransactionalComponent
      with BulkImportStudentAwardsForUniversityIdsDescription
      with BulkImportStudentAwardsForUniversityIdsRequest {
      override val universityIds: Seq[String] = ids
    }
}

abstract class BulkImportStudentAwardsCommandInternal extends CommandInternal[Result] with TaskBenchmarking with PropertyCopying {
  self: BulkImportStudentAwardsRequest
    with StudentAwardServiceComponent
    with AwardServiceComponent
    with ClassificationServiceComponent
    with TransactionalComponent =>

  lazy val awardsMap = awardService.allAwards.map(a => a.code -> a).toMap
  lazy val classificationMap = classificationService.allClassifications.map(c => c.code -> c).toMap

 // Maybe we should do something in the generic copyObjectProperty to deal with optional bean property value.
  private def copyClassification(newCode: Option[String], studentAwardBean: BeanWrapper, classification: Option[Classification]) = {
    val property = "classification"
    val oldValue = studentAwardBean.getPropertyValue(property) match {
      case None => None
      case classification: Option[Classification] => classification
    }

    if ((oldValue == None && newCode == None) || (oldValue.isDefined && newCode.isDefined && oldValue.get.code == newCode.get)) false
    else {
      logger.debug(s"Detected property change for $property: $oldValue -> $newCode; setting value")
      studentAwardBean.setPropertyValue(property, classification.orNull)
      true
    }
  }

  private def copyProperties(row: StudentAwardRow, studentAward: StudentAward): Boolean = {
    val studentAwardBean = PropertyAccessorFactory.forBeanPropertyAccess(studentAward)
    lazy val classification = row.classificationCode.flatMap(classificationMap.get)

    copyOptionProperty(studentAwardBean, "awardDate", row.awardDate) |
      copyClassification(row.classificationCode, studentAwardBean, classification)
  }


  def getStudentAward(saRow: StudentAwardRow) = {
    val sa = new StudentAward
    sa.sprCode = saRow.sprCode
    sa.academicYear = saRow.academicYear
    sa.award = awardsMap.get(saRow.awardCode).get
    sa.awardDate = saRow.awardDate.orNull
    sa.classification = saRow.classificationCode.flatMap(code => classificationMap.get(code)).orNull
    sa
  }

  override def applyInternal(): Result = transactional() {
    val rows = allRows

    var created: Int = 0
    var updated: Int = 0

    // studentAwards that we found in SITS that already existed in Tabula
    val foundStudentAwards = benchmarkTask("Updating student awards") {
      rows.flatMap { row =>
        val existing = existingStudentAwards.get((row.sprCode, row.academicYear.toString, row.awardCode))
        val studentAward = existing.getOrElse(getStudentAward(row))

        lazy val hasChanged = copyProperties(row, studentAward)

        if (existing.isEmpty || hasChanged) {
          logger.debug(s"Saving changes for $studentAward because ${if (existing.isEmpty) "it's a new object" else "it's changed"}")

          studentAwardService.saveOrUpdate(studentAward)

          if (existing.isEmpty) created += 1
          else updated += 1
        }
        existing
      }
    }
    val deleted = benchmarkTask("Deleting student awards that weren't found in SITS") {
      val missingStudentAwards = existingStudentAwards.values.toSet -- foundStudentAwards
      missingStudentAwards.foreach { sa =>
        logger.debug(s"Deleting $sa")
        studentAwardService.delete(sa)
      }

      missingStudentAwards.size
    }
    logger.info(s"Student Award Import completed;created-$created, updated-$updated, deleted-$deleted")

    Result(created, updated, deleted)
  }
}

trait BulkImportStudentAwardsRequest {
  def allRows: Seq[StudentAwardRow]

  def existingStudentAwards: Map[(String, String, String), StudentAward]
}

trait BulkImportStudentAwardsForAcademicYearRequest extends BulkImportStudentAwardsRequest with TaskBenchmarking {
  self: StudentAwardImporterComponent
    with StudentAwardServiceComponent =>

  def academicYear: AcademicYear

  def yearsToImport: Seq[AcademicYear] = academicYear.yearsSurrounding(3, 0)

  lazy val allRows: Seq[StudentAwardRow] = studentAwardImporter.getStudentAwardRowsForAcademicYears(yearsToImport)

  lazy val existingStudentAwards: Map[(String, String, String), StudentAward] = benchmarkTask("Fetching existing student awards") {
    studentAwardService.getByAcademicYears(yearsToImport)
      .map { sa => (sa.sprCode, sa.academicYear.toString, sa.award.code) -> sa }
      .toMap
  }
}

trait BulkImportStudentAwardsForUniversityIdsRequest extends BulkImportStudentAwardsRequest with TaskBenchmarking {
  self: StudentAwardImporterComponent
    with StudentAwardServiceComponent =>

  def universityIds: Seq[String]

  lazy val allRows: Seq[StudentAwardRow] = studentAwardImporter.getStudentAwardRowsForUniversityIds(universityIds)

  lazy val existingStudentAwards: Map[(String, String, String), StudentAward] = benchmarkTask("Fetching existing student awards") {
    studentAwardService.getByUniversityIds(universityIds)
      .map { sa => (sa.sprCode, sa.academicYear.toString, sa.award.code) -> sa }
      .toMap
  }
}

trait BulkImportStudentAwardsDescription extends Describable[Result] {
  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "studentAwardsAdded" -> result.created,
      "studentAwardsChanged" -> result.updated,
      "studentAwardsDeleted" -> result.deleted,
    )
}

trait BulkImportStudentAwardsForAcademicYearDescription extends BulkImportStudentAwardsDescription {
  self: BulkImportStudentAwardsForAcademicYearRequest =>

  override lazy val eventName: String = "BulkImportStudentAwardsForAcademicYear"

  override def describe(d: Description): Unit =
    d.property("academicYears" -> yearsToImport.map(_.toString))
}

trait BulkImportStudentAwardsForUniversityIdsDescription extends BulkImportStudentAwardsDescription {
  self: BulkImportStudentAwardsForUniversityIdsRequest =>

  override lazy val eventName: String = "BulkImportStudentAwardsForUniversityIds"

  override def describe(d: Description): Unit =
    d.studentIds(universityIds)
}
