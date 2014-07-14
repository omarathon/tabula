package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointType, AttendanceMonitoringPointStyle, AttendanceMonitoringPoint, AttendanceMonitoringTemplate, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import uk.ac.warwick.tabula.commands._
import org.joda.time.DateTime
import org.springframework.validation.Errors


object AddTemplatePointsToSchemesCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new AddTemplatePointsToSchemesCommandInternal(department, academicYear)
		with ComposableCommand[Seq[AttendanceMonitoringPoint]]
		with AddTemplatePointsToSchemesCommandState
		with AddTemplatePointsToSchemesPermissions
		with AutowiringAttendanceMonitoringServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringTermServiceComponent
		with AddTemplatePointsToSchemesDescription
		with AddTemplatePointsToSchemesValidation
}

class AddTemplatePointsToSchemesCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] with TaskBenchmarking {
	self: AddTemplatePointsToSchemesCommandState with AttendanceMonitoringServiceComponent  with ProfileServiceComponent with TermServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringPoint] = {

		val attendanceMonitoringPoints = attendanceMonitoringService.generatePointsFromTemplateScheme(templateScheme, academicYear)

		val newPoints = schemes.asScala.flatMap { scheme =>
				attendanceMonitoringPoints.map { point =>
				val newPoint = point.cloneTo(scheme)
				newPoint.pointType = AttendanceMonitoringPointType.Standard
				newPoint.createdDate = new DateTime()
				newPoint.updatedDate = new DateTime()
				attendanceMonitoringService.saveOrUpdate(newPoint)
				newPoint
			}
		}

		val students = profileService.getAllMembersWithUniversityIds(schemes.asScala.flatMap(_.members.members).distinct).flatMap {
			case student: StudentMember => Option(student)
			case _ => None
		}
		attendanceMonitoringService.updateCheckpointTotalsAsync(students, department, academicYear)

		newPoints
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

	def templateSchemeItems() = attendanceMonitoringService.listTemplateSchemesByStyle(schemes.get(0).pointStyle)
}

trait AddTemplatePointsToSchemesDescription extends Describable[Seq[AttendanceMonitoringPoint]] {
	self: AddTemplatePointsToSchemesCommandState =>

	override lazy val eventName = "AddTemplatePointsToScheme"
	override def describe(d: Description) {
		schemes.asScala.foreach(d.attendanceMonitoringScheme)
	}
}


trait AddTemplatePointsToSchemesValidation extends AttendanceMonitoringPointValidation with SelfValidating {
	self: AddTemplatePointsToSchemesCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def validate(errors: Errors) {
		if (templateScheme == null) {
			errors.reject("attendanceMonitoringPoints.templateScheme.Empty")
		} else {
			validateSchemePointStyles(errors, templateScheme.pointStyle, schemes.asScala)

			attendanceMonitoringService.generatePointsFromTemplateScheme(templateScheme, academicYear).foreach{ point =>
				templateScheme.pointStyle match {
					case AttendanceMonitoringPointStyle.Date =>
						validateDuplicateForDate(errors, point.name, point.startDate, point.endDate, schemes.asScala, global = true)
					case AttendanceMonitoringPointStyle.Week =>
						validateDuplicateForWeek(errors, point.name, point.startWeek, point.endWeek, schemes.asScala, global = true)
				}
			}

		}
	}

}
