package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.imports.BulkImportModuleRegistrationsCommand._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationImporter.ModuleRegistrationsByAcademicYearQuery
import uk.ac.warwick.tabula.services.scheduling.{AutowiringSitsDataSourceComponent, CopyModuleRegistrationProperties, SitsDataSourceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleAndDepartmentServiceComponent, ModuleRegistrationServiceComponent}

import scala.collection.immutable.HashMap
import scala.jdk.CollectionConverters._

object BulkImportModuleRegistrationsCommand {
  case class Result(created: Int, updated: Int, deleted: Int)
  type Command = Appliable[Result] with BulkImportModuleRegistrationsState

  def apply(academicYear: AcademicYear): Command =
    new BulkImportModuleRegistrationsCommandInternal(academicYear)
      with ComposableCommand[Result]
      with ImportSystemDataPermissions
      with CopyModuleRegistrationProperties
      with PropertyCopying
      with AutowiringSitsDataSourceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with BulkImportModuleRegistrationsDescription
}

abstract class BulkImportModuleRegistrationsCommandInternal(val academicYear: AcademicYear) extends CommandInternal[Result] with BulkImportModuleRegistrationsState
  with TaskBenchmarking {
  self: SitsDataSourceComponent
    with ModuleRegistrationServiceComponent
    with ModuleAndDepartmentServiceComponent
    with CopyModuleRegistrationProperties =>

  def applyInternal(): Result = transactional() {
    val params = HashMap(("academicYear", academicYear)).asJava
    val rows = benchmarkTask(s"Fetching registrations from SITS") {
      new ModuleRegistrationsByAcademicYearQuery(sitsDataSource).executeByNamedParam(params).asScala.distinct
    }

    val modulesBySitsCode = benchmarkTask(s"Fetching Tabula modules") {
      rows
        .groupBy(_.sitsModuleCode)
        .keys
        .flatMap { sitsModuleCode =>
          moduleAndDepartmentService.getModuleBySitsCode(sitsModuleCode).map(m => (sitsModuleCode, m))
        }.toMap
    }

    // key the existing registrations by scj code and module to make finding them faster
    val existingRegistrations =  benchmarkTask(s"Fetching existing module registrations") {
      moduleRegistrationService.getByYear(academicYear)
    }
    val existingRegistrationsGrouped = benchmarkTask(s"Keying module registrations by scj and module code") {
      existingRegistrations.groupBy(mr => (mr._scjCode, mr.module.code))
    }

    var created: Int = 0
    var updated: Int = 0

    // registrations that we found in SITS that already existed in Tabula (don't delete these)
    val foundRegistrations = benchmarkTask(s"Updating registrations") { rows.flatMap(row => {
      val existing = existingRegistrationsGrouped.getOrElse((row.scjCode, row.moduleCode.orNull), Nil).find(row.matches)
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

    val deleted = benchmarkTask(s"Deleting registrations that weren't found in SITS") {
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

trait BulkImportModuleRegistrationsState {
  val academicYear: AcademicYear
}

trait BulkImportModuleRegistrationsDescription extends Describable[Result] {
  self: BulkImportModuleRegistrationsState =>

  override lazy val eventName: String = "BulkImportModuleRegistrations"

  override def describe(d: Description): Unit =
    d.property("academicYear" -> academicYear.toString)

  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "moduleRegistrationsAdded" -> result.created,
      "moduleRegistrationsChanged" -> result.updated,
      "moduleRegistrationsDeleted" -> result.deleted,
    )
}
