package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.attendance.commands.ViewMonitoringPointSetsCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.TemplateMethodModelEx
import scala.collection.JavaConverters._
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel

/**
 * Displays the screen for viewing monitoring points for a department
 */
@Controller
@RequestMapping(Array("/{dept}"))
class ViewMonitoringPointsForDeptController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable dept: Department,
		@RequestParam(value="academicYear", required = false) academicYear: AcademicYear,
		@RequestParam(value="route", required = false) route: Route,
		@RequestParam(value="set", required = false) set: MonitoringPointSet
	) =
			ViewMonitoringPointSetsCommand(user, dept, Option(academicYear), Option(route), Option(set))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[StudentMember, Map[MonitoringPoint, String]]],
		@RequestParam(value="updatedMonitoringPoint", required = false) updatedPoint: MonitoringPoint
	) = {		
		val membersWithMissedOrLateCheckpoints = cmd.apply()
		val m = (membersWithMissedOrLateCheckpoints.apply _).apply _
		
		Mav("home/view", 
			"membersWithMissedOrLateCheckpoints" -> membersWithMissedOrLateCheckpoints,
			"missedCheckpointsByMember" -> new MissedCheckpointsByMemberTemplateMethodModel(membersWithMissedOrLateCheckpoints),
			"missedCheckpointsByMemberByPoint" -> new MissedCheckpointsByMemberByPointTemplateMethodModel(membersWithMissedOrLateCheckpoints),
			"updatedPoint" -> updatedPoint
		)
	}

}

class MissedCheckpointsByMemberTemplateMethodModel(membersWithMissedOrLateCheckpoints: Map[StudentMember, Map[MonitoringPoint, String]]) 
	extends TemplateMethodModelEx {
	
	override def exec(list: JList[_]) = {
		val args = list.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }	
		
		args match {
			case Seq(student: StudentMember) => membersWithMissedOrLateCheckpoints(student)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}
}

class MissedCheckpointsByMemberByPointTemplateMethodModel(membersWithMissedOrLateCheckpoints: Map[StudentMember, Map[MonitoringPoint, String]]) 
	extends TemplateMethodModelEx {
	
	override def exec(list: JList[_]) = {
		val args = list.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }	
		
		args match {
			case Seq(student: StudentMember, point: MonitoringPoint) => membersWithMissedOrLateCheckpoints(student)(point)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}
}