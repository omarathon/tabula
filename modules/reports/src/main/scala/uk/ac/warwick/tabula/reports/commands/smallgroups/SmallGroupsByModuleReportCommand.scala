package uk.ac.warwick.tabula.reports.commands.smallgroups

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.reports.commands.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.userlookup.User

object SmallGroupsByModuleReportCommand {
	def apply(department: Department, academicYear: AcademicYear, filteredAttendance: AllSmallGroupsReportCommandResult) =
		new SmallGroupsByModuleReportCommandInternal(department, academicYear, filteredAttendance)
			with ComposableCommand[SmallGroupsByModuleReportCommandResult]
			with ReportPermissions
			with ReportCommandState
			with ReadOnly with Unaudited
}

case class SmallGroupsByModuleReportCommandResult(
	counts: Map[User, Map[Module, Int]],
	students: Seq[User],
	modules: Seq[Module]
)

class SmallGroupsByModuleReportCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val filteredAttendance: AllSmallGroupsReportCommandResult
) extends CommandInternal[SmallGroupsByModuleReportCommandResult] {

	override def applyInternal() = {
		val byModule: Map[User, Map[Module, Int]] = filteredAttendance.attendance.map{case(student, eventMap) =>
			student -> eventMap.groupBy(_._1.event.group.groupSet.module).map { case (module, groupedEventMap) =>
				module -> groupedEventMap.keys.size
			}
		}

		SmallGroupsByModuleReportCommandResult(
			byModule,
			byModule.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)),
			byModule.flatMap(_._2.map(_._1)).toSeq.sorted
		)
	}
}
