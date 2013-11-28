package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.commands.{Describable, ComposableCommand, CommandInternal, Description}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.{TermService, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.scheduling.services.{AutowiringExportAttendanceToSitsServiceComponent, ExportAttendanceToSitsServiceComponent}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.helpers.Logging

object ExportAttendanceToSitsCommand {
	def apply() = new ExportAttendanceToSitsCommand
		with ComposableCommand[Unit]
		with ExportAttendanceToSitsCommandPermissions
		with ExportAttendanceToSitsCommandDescription
		with AutowiringMonitoringPointServiceComponent
		with AutowiringExportAttendanceToSitsServiceComponent
}

class ExportAttendanceToSitsCommand extends CommandInternal[Unit] with Logging {

	self: MonitoringPointServiceComponent with ExportAttendanceToSitsServiceComponent =>

	override def applyInternal() = {
		// get the reports from MPService
		val unreportedReports = monitoringPointService.findUnreportedReports

		// for each student
		unreportedReports.groupBy(_.student).foreach{case(student, reportList) =>
			// group reports by academic year
			val reportsByAcademicYear = reportList.groupBy(_.academicYear)
			// for each academic year in order
			reportsByAcademicYear.keys.toSeq.sortBy(_.startYear).foreach{academicYear =>
				val reportsInAcademicYear = reportsByAcademicYear(academicYear)
				// push a report for each term in order (if a report exists)
				TermService.orderedTermNames.foreach(term => {
					reportsInAcademicYear.find(_.monitoringPeriod == term) match {
						case None =>
						case Some(report: MonitoringPointReport) => {
							val result = exportAttendanceToSitsService.exportToSits(report)
							if (result)
								transactional() {
									monitoringPointService.markReportAsPushed(report)
									logger.info(s"Reported ${report.missed} missed points for ${report.student.universityId} for ${report.monitoringPeriod}")
								}
							else
								logger.error(s"Could not push monitoring report to SITS for ${report.student.universityId}")
						}
					}
				})
			}
		}
	}

}

trait ExportAttendanceToSitsCommandPermissions extends RequiresPermissionsChecking {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Export)
	}
}

trait ExportAttendanceToSitsCommandDescription extends Describable[Unit] {
	override def describe(d: Description) {}
}