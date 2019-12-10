package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.imports.BulkImportModuleRegistrationsCommand._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.services.scheduling.{AutowiringModuleRegistrationImporterComponent, CopyModuleRegistrationProperties, ModuleRegistrationImporterComponent, ModuleRegistrationRow}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleAndDepartmentServiceComponent, ModuleRegistrationServiceComponent}

object BulkImportModuleRegistrationsCommand {
  case class Result(created: Int, updated: Int, deleted: Int)
  type Command = Appliable[Result]

  def apply(year: AcademicYear): Command =
    new BulkImportModuleRegistrationsCommandInternal
      with ComposableCommand[Result]
      with ImportSystemDataPermissions
      with CopyModuleRegistrationProperties
      with PropertyCopying
      with AutowiringModuleRegistrationImporterComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with BulkImportModuleRegistrationsForAcademicYearDescription
      with BulkImportModuleRegistrationsForAcademicYearRequest {
      override val academicYear: AcademicYear = year
    }

  def apply(ids: Seq[String]): Command =
    new BulkImportModuleRegistrationsCommandInternal
      with ComposableCommand[Result]
      with ImportSystemDataPermissions
      with CopyModuleRegistrationProperties
      with PropertyCopying
      with AutowiringModuleRegistrationImporterComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with BulkImportModuleRegistrationsForUniversityIdsDescription
      with BulkImportModuleRegistrationsForUniversityIdsRequest {
      override val universityIds: Seq[String] = ids
    }
}

abstract class BulkImportModuleRegistrationsCommandInternal extends CommandInternal[Result] with TaskBenchmarking {
  self: BulkImportModuleRegistrationsRequest
    with ModuleRegistrationServiceComponent
    with ModuleAndDepartmentServiceComponent
    with CopyModuleRegistrationProperties =>

  def applyInternal(): Result = transactional() {
    val rows = benchmarkTask("Combine duplicate rows with best guesses") {
      allRows.groupBy(_.notionalKey).view.mapValues { duplicates =>
        if (duplicates.size == 1) duplicates.head
        else ModuleRegistrationRow.combine(duplicates.toSeq)
      }.values
    }

    val modulesBySitsCode = benchmarkTask("Fetching Tabula modules") {
      rows
        .groupBy(_.sitsModuleCode)
        .keys
        .flatMap { sitsModuleCode =>
          moduleAndDepartmentService.getModuleBySitsCode(sitsModuleCode).map(m => (sitsModuleCode, m))
        }.toMap
    }

    // key the existing registrations by scj code and module to make finding them faster
    val existingRegistrationsGrouped = benchmarkTask("Keying module registrations by scj, module code and academic year") {
      existingRegistrations.groupBy(mr => (mr._scjCode, mr.module.code, mr.academicYear.toString))
    }

    var created: Int = 0
    var updated: Int = 0

    // registrations that we found in SITS that already existed in Tabula (don't delete these)
    val foundRegistrations = benchmarkTask("Updating registrations") { rows.flatMap(row => {
      val existing = existingRegistrationsGrouped.getOrElse((row.scjCode, row.moduleCode.orNull, row.academicYear), Nil).find(row.matches)
      val registration = existing.orElse(modulesBySitsCode.get(row.sitsModuleCode).map(row.toModuleRegistration))

      registration match {
        case Some(r) =>
          val hasChanged = copyProperties(row, r)

          if (existing.isEmpty || hasChanged || r.deleted) {
            logger.debug(s"Saving changes for $registration because ${if (existing.isEmpty) "it's a new object" else if (hasChanged) "it's changed" else "it's been un-deleted"}")

            r.deleted = false
            r.lastUpdatedDate = DateTime.now
            moduleRegistrationService.saveOrUpdate(r)

            if (existing.isEmpty) created += 1
            else updated += 1
          }
        case None => logger.warn(s"No module exists in Tabula for $row - Not importing")
      }

      existing
    })}

    val deleted = benchmarkTask("Deleting registrations that weren't found in SITS") {
      val missingRegistrations = (existingRegistrations.toSet -- foundRegistrations).filterNot(_.deleted)
      missingRegistrations.foreach(mr => {
        logger.debug(s"Marking $mr as deleted")
        mr.markDeleted()
        mr.lastUpdatedDate = DateTime.now
        moduleRegistrationService.saveOrUpdate(mr)
      })

      missingRegistrations.size
    }

    Result(created, updated, deleted)
  }
}

trait BulkImportModuleRegistrationsRequest {
  def allRows: Seq[ModuleRegistrationRow]
  def existingRegistrations: Seq[ModuleRegistration]
}

trait BulkImportModuleRegistrationsForAcademicYearRequest extends BulkImportModuleRegistrationsRequest with TaskBenchmarking {
  self: ModuleRegistrationImporterComponent
    with ModuleRegistrationServiceComponent =>

  def academicYear: AcademicYear

  def yearsToImport: Seq[AcademicYear] = Seq(academicYear, academicYear.previous)

  lazy val allRows: Seq[ModuleRegistrationRow] = moduleRegistrationImporter.getModuleRegistrationRowsForAcademicYears(yearsToImport)

  lazy val existingRegistrations: Seq[ModuleRegistration] =  benchmarkTask("Fetching existing module registrations") {
    moduleRegistrationService.getByYears(yearsToImport)
  }
}

trait BulkImportModuleRegistrationsForUniversityIdsRequest extends BulkImportModuleRegistrationsRequest with TaskBenchmarking {
  self: ModuleRegistrationImporterComponent
    with ModuleRegistrationServiceComponent =>

  def universityIds: Seq[String]

  lazy val allRows: Seq[ModuleRegistrationRow] = moduleRegistrationImporter.getModuleRegistrationRowsForUniversityIds(universityIds)

  lazy val existingRegistrations: Seq[ModuleRegistration] =  benchmarkTask("Fetching existing module registrations") {
    moduleRegistrationService.getByUniversityIds(universityIds)
  }
}

trait BulkImportModuleRegistrationsDescription extends Describable[Result] {
  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "moduleRegistrationsAdded" -> result.created,
      "moduleRegistrationsChanged" -> result.updated,
      "moduleRegistrationsDeleted" -> result.deleted,
    )
}

trait BulkImportModuleRegistrationsForAcademicYearDescription extends BulkImportModuleRegistrationsDescription {
  self: BulkImportModuleRegistrationsForAcademicYearRequest =>

  override lazy val eventName: String = "BulkImportModuleRegistrationsForAcademicYear"

  override def describe(d: Description): Unit =
    d.property("academicYears" -> yearsToImport.map(_.toString))
}

trait BulkImportModuleRegistrationsForUniversityIdsDescription extends BulkImportModuleRegistrationsDescription {
  self: BulkImportModuleRegistrationsForUniversityIdsRequest =>

  override lazy val eventName: String = "BulkImportModuleRegistrationsForUniversityIds"

  override def describe(d: Description): Unit =
    d.studentIds(universityIds)
}
