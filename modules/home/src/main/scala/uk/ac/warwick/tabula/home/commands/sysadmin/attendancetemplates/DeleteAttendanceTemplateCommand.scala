package uk.ac.warwick.tabula.home.commands.sysadmin.attendancetemplates

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}

object DeleteAttendanceTemplateCommand {
	def apply(template: AttendanceMonitoringTemplate) =
		new DeleteAttendanceTemplateCommandInternal(template)
			with ComposableCommand[Unit]
			with AutowiringAttendanceMonitoringServiceComponent
			with DeleteAttendanceTemplateValidation
			with DeleteAttendanceTemplateDescription
			with AttendanceTemplatePermissions
			with DeleteAttendanceTemplateCommandState
}


class DeleteAttendanceTemplateCommandInternal(val template: AttendanceMonitoringTemplate) extends CommandInternal[Unit] {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		attendanceMonitoringService.deleteTemplate(template)
	}

}

trait DeleteAttendanceTemplateValidation extends SelfValidating {

	self: DeleteAttendanceTemplateCommandState =>

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "attendanceMonitoringTemplate.delete.confirm")
	}

}

trait DeleteAttendanceTemplateDescription extends Describable[Unit] {

	self: DeleteAttendanceTemplateCommandState =>

	override lazy val eventName = "DeleteAttendanceTemplate"

	override def describe(d: Description) {
		d.attendanceMonitoringTemplate(template)
	}
}

trait DeleteAttendanceTemplateCommandState {
	def template: AttendanceMonitoringTemplate

	// Bind variables
	var confirm = false
}
