package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._

trait MonitoringPointValidation {

	def validateName(errors: Errors, name: String, bindPoint: String) {
		if (!name.hasText) {
			errors.rejectValue(bindPoint, "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue(bindPoint, "monitoringPoint.name.toolong")
		}
	}

	def validateWeek(errors: Errors, week: Int, bindPoint: String) {
		week match {
			case y if y < 1  => errors.rejectValue(bindPoint, "monitoringPoint.week.min")
			case y if y > 52 => errors.rejectValue(bindPoint, "monitoringPoint.week.max")
			case _ =>
		}
	}

	def validateWeeks(errors: Errors, validFromWeek: Int, requiredFromWeek: Int, bindPoint: String) {
		if (validFromWeek > requiredFromWeek) {
			errors.rejectValue(bindPoint, "monitoringPoint.weeks")
		}
	}
}
