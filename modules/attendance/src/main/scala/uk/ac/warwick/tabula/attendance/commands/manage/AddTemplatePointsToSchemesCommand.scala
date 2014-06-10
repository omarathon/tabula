package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringTemplate, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import uk.ac.warwick.tabula.commands.{Description, Describable, ComposableCommand, CommandInternal}
import org.joda.time.DateTime


object AddTemplatePointsToSchemesCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new AddTemplatePointsToSchemesCommandInternal(department, academicYear)
		with ComposableCommand[Seq[AttendanceMonitoringScheme]]
		with AddTemplatePointsToSchemesCommandState
		with AddTemplatePointsToSchemesPermissions
		with AutowiringAttendanceMonitoringServiceComponent
		with AutowiringTermServiceComponent
		with AddTemplatePointsToSchemesDescription
}

class AddTemplatePointsToSchemesCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[AttendanceMonitoringScheme]] {
	self: AddTemplatePointsToSchemesCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringScheme] = {

		val attendanceMonitoringPoints = AttendancePointsFromTemplateSchemeCommand(templateScheme, academicYear, department).apply()
		val schemeSeq = schemes.asScala

		schemeSeq.foreach { scheme =>
			attendanceMonitoringPoints.foreach { point =>
				val newPoint = point.cloneTo(scheme)
				newPoint.createdDate = new DateTime()
				newPoint.updatedDate = new DateTime()
				attendanceMonitoringService.saveOrUpdate(newPoint)
			}
		}
		schemeSeq
	}
}

trait AddTemplatePointsToSchemesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddTemplatePointsToSchemesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) = {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}
}

trait AddTemplatePointsToSchemesCommandState {
	self: AttendanceMonitoringServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	var schemes: JList[AttendanceMonitoringScheme] = new JArrayList()
	var templateScheme: AttendanceMonitoringTemplate = _

	def templateSchemeItems() = {
		val style = schemes.get(0).pointStyle
		attendanceMonitoringService.listTemplateSchemesByStyle(style)
	}
}

trait AddTemplatePointsToSchemesDescription extends Describable[Seq[AttendanceMonitoringScheme]] {
	self: AddTemplatePointsToSchemesCommandState =>

	override lazy val eventName = "AddTemplatePointsToScheme"
	override def describe(d: Description) {
		schemes.asScala.foreach(d.attendanceMonitoringScheme)
	}
}
