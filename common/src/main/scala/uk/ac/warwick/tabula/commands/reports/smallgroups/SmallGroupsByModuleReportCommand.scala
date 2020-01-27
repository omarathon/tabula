package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.userlookup.User

object SmallGroupsByModuleReportCommand {
  def apply(department: Department, academicYear: AcademicYear) =
    new SmallGroupsByModuleReportCommandInternal(department, academicYear)
      with ComposableCommand[SmallGroupsByModuleReportCommandResult]
      with ReportPermissions
      with SmallGroupsByModuleReportCommandState
      with SetsFilteredAttendance
      with ReadOnly with Unaudited
}

case class SmallGroupsByModuleReportCommandResult(
  counts: Map[User, Map[Module, Int]],
  studentDatas: Seq[AttendanceMonitoringStudentData],
  modules: Seq[Module],
  reportRangeStartDate: LocalDate,
  reportRangeEndDate: LocalDate
)

class SmallGroupsByModuleReportCommandInternal(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[SmallGroupsByModuleReportCommandResult] {

  self: SmallGroupsByModuleReportCommandState =>

  override def applyInternal(): SmallGroupsByModuleReportCommandResult = {
    val byModule: Map[User, Map[Module, Int]] = filteredAttendance.attendance.map { case (student, eventMap) =>
      val allModules = filteredAttendance.relevantEvents(student).map(_.event.group.groupSet.module)
      val filteredModules = eventMap.groupBy(_._1.event.group.groupSet.module)

      student -> allModules.map { module =>
        module -> filteredModules.get(module).map(_.size).getOrElse(0)
      }.toMap
    }

    SmallGroupsByModuleReportCommandResult(
      byModule,
      filteredAttendance.studentDatas,
      byModule.flatMap(_._2.keys).toSeq.distinct.sorted,
      startDate, endDate
    )
  }
}

trait SetsFilteredAttendance {

  self: SmallGroupsByModuleReportCommandState =>

  def setFilteredAttendance(theFilteredAttendance: AllSmallGroupsReportCommandResult): Unit = {
    filteredAttendance = theFilteredAttendance
  }
}

trait SmallGroupsByModuleReportCommandState extends ReportCommandState with ReportCommandRequest {
  var filteredAttendance: AllSmallGroupsReportCommandResult = _
}
