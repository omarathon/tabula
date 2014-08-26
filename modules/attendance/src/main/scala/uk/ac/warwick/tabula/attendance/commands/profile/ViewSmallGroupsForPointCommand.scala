package uk.ac.warwick.tabula.attendance.commands.profile

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class ViewSmallGroupsForPointCommandResultCourse(
	name: String,
	route: String,
	department: String,
	status: String,
	attendance: String,
	courseType: String,
	yearOfStudy: String
)

case class ViewSmallGroupsForPointCommandResultModule(
	hasGroups: Boolean,
	code: String,
	title: String,
	department: String,
	cats: String,
	status: String
)

case class ViewSmallGroupsForPointCommandResult(
	course: ViewSmallGroupsForPointCommandResultCourse,
	modules: Seq[ViewSmallGroupsForPointCommandResultModule]
)

object ViewSmallGroupsForPointCommand {
	def apply(student: StudentMember, point: AttendanceMonitoringPoint) =
		new ViewSmallGroupsForPointCommandInternal(student, point)
			with AutowiringSmallGroupServiceComponent
			with ComposableCommand[ViewSmallGroupsForPointCommandResult]
			with ViewSmallGroupsForPointPermissions
			with ViewSmallGroupsForPointCommandState
			with ReadOnly with Unaudited
}


class ViewSmallGroupsForPointCommandInternal(val student: StudentMember, val point: AttendanceMonitoringPoint)
	extends CommandInternal[ViewSmallGroupsForPointCommandResult] {

	self: SmallGroupServiceComponent =>

	override def applyInternal() = {
		ViewSmallGroupsForPointCommandResult(
			student.mostSignificantCourseDetails match {
				case None => ViewSmallGroupsForPointCommandResultCourse("","",student.homeDepartment.name,"","","","")
				case Some(scd) =>
					ViewSmallGroupsForPointCommandResultCourse(
						student.mostSignificantCourseDetails.map(scd => scd.course.name).getOrElse(""),
						student.mostSignificantCourseDetails.map(scd => s"${scd.route.name} (${scd.route.code.toUpperCase})").getOrElse(""),
						student.homeDepartment.name,
						student.mostSignificantCourseDetails.map(scd => scd.statusOnRoute.fullName.toLowerCase.capitalize).getOrElse(""),
						student.mostSignificantCourseDetails.map(scd => scd.latestStudentCourseYearDetails.modeOfAttendance.fullNameAliased).getOrElse(""),
						student.mostSignificantCourseDetails.map(scd => scd.route.degreeType.toString).getOrElse(""),
						student.mostSignificantCourseDetails.map(scd => scd.latestStudentCourseYearDetails.yearOfStudy.toString).getOrElse("")
					)
			},
			student.mostSignificantCourseDetails.flatMap(scd =>
				scd.freshStudentCourseYearDetails.find(_.academicYear == point.scheme.academicYear).map(scyd =>
					scyd.moduleRegistrations.map{moduleRegistration =>
						ViewSmallGroupsForPointCommandResultModule(
							smallGroupService.hasSmallGroups(moduleRegistration.module),
							moduleRegistration.module.code.toUpperCase,
							moduleRegistration.module.name,
							moduleRegistration.module.department.name,
							moduleRegistration.cats.toString,
							Option(moduleRegistration.selectionStatus).map(_.description).getOrElse("")
						)
					}
				)
			).getOrElse(Seq())
		)
	}

}

trait ViewSmallGroupsForPointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewSmallGroupsForPointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}

}

trait ViewSmallGroupsForPointCommandState {
	def student: StudentMember
	def point: AttendanceMonitoringPoint
}
