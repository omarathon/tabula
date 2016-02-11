package uk.ac.warwick.tabula.web.controllers.attendance.check

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.attendance.check.{CheckEventAttendanceCheckpointsCommand, CheckpointResult}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/attendance/check/smallgroup", "/attendance/smallgroupcheckpoints"))
class CheckEventAttendanceCheckpointsController extends AttendanceController{

	@ModelAttribute("command")
	def command(@RequestParam occurrence: SmallGroupEventOccurrence) =
		CheckEventAttendanceCheckpointsCommand(mandatory(occurrence))

	@RequestMapping(method = Array(POST))
	def home(@ModelAttribute("command") cmd: Appliable[Seq[CheckpointResult]]) = {
		val checkpoints = cmd.apply()
		Mav(
			new JSONView(
				checkpoints.groupBy(_.student).map{case(_, c) => c.head}.toSeq.sortBy(_.student.lastName).map(checkpoint => {
					Map(
						"name" -> checkpoint.student.fullName.getOrElse(checkpoint.student.universityId),
						"universityId" -> checkpoint.student.universityId
					)
				})
			)
		)
	}

}
