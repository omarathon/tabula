package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, ComposableCommand, CommandInternal, Appliable}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringCheckpoint}
import uk.ac.warwick.tabula.web.views.JSONView
import scala.util.parsing.json.JSONObject
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendance, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointGroupProfileServiceComponent, MonitoringPointGroupProfileServiceComponent}
import scala.collection.JavaConverters._

object SmallGroupCheckpointsCommand {
	def apply(occurrence: SmallGroupEventOccurrence) = new SmallGroupCheckpointsCommand(occurrence)
		with ComposableCommand[Seq[MonitoringCheckpoint]]
		with AutowiringMonitoringPointGroupProfileServiceComponent
		with SmallGroupCheckpointsPermissions
		with SmallGroupCheckpointsCommandState
		with ReadOnly with Unaudited

}

class SmallGroupCheckpointsCommand(val occurrence: SmallGroupEventOccurrence)
	extends CommandInternal[Seq[MonitoringCheckpoint]] with SmallGroupCheckpointsCommandState {

	self: MonitoringPointGroupProfileServiceComponent =>

	def applyInternal() = {
		monitoringPointGroupProfileService.getCheckpointsForAttendance(attendances.asScala.map{ case (universityId, state) =>
			val attendance = new SmallGroupEventAttendance
			attendance.occurrence = occurrence
			attendance.state = state
			attendance.universityId = universityId
			attendance
		}.toSeq)
	}

}

trait SmallGroupCheckpointsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: SmallGroupCheckpointsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, occurrence)
	}

}

trait SmallGroupCheckpointsCommandState {
	def occurrence: SmallGroupEventOccurrence
	var attendances: JMap[String, AttendanceState] = JHashMap()
}

@Controller
@RequestMapping(Array("/smallgroupcheckpoints"))
class SmallGroupCheckpointsController extends AttendanceController {

	@ModelAttribute("command")
	def command(@RequestParam occurrence: SmallGroupEventOccurrence) = SmallGroupCheckpointsCommand(occurrence)

	@RequestMapping(method = Array(POST))
	def checkpoints(@ModelAttribute("command") cmd: Appliable[Seq[MonitoringCheckpoint]]) = {
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
