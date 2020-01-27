package uk.ac.warwick.tabula.commands.attendance.view

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.GroupedPoint
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

object RecordMonitoringPointCommand {
  type Result = (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
  type Command = Appliable[Result] with SelfValidating with PopulateOnForm with SetFilterPointsResultOnRecordMonitoringPointCommand

  def apply(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint, user: CurrentUser): Command =
    new RecordMonitoringPointCommandInternal(department, academicYear, templatePoint, user)
      with ComposableCommand[Result]
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringSecurityServiceComponent
      with RecordMonitoringPointValidation
      with RecordMonitoringPointDescription
      with RecordMonitoringPointPermissions
      with RecordMonitoringPointCommandState
      with PopulateRecordMonitoringPointCommand
      with SetFilterPointsResultOnRecordMonitoringPointCommand
      with MissedAttendanceMonitoringCheckpointsNotifications
}


class RecordMonitoringPointCommandInternal(val department: Department, val academicYear: AcademicYear, val templatePoint: AttendanceMonitoringPoint, val user: CurrentUser)
  extends CommandInternal[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

  self: RecordMonitoringPointCommandState with AttendanceMonitoringServiceComponent =>

  override def applyInternal(): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
    checkpointMap.asScala.map { case (student, pointMap) =>
      attendanceMonitoringService.setAttendance(student, pointMap.asScala.toMap, user)
    }.toSeq.foldLeft(
      (Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())
    ) {
      case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
    }
  }

}

trait SetFilterPointsResultOnRecordMonitoringPointCommand {

  self: RecordMonitoringPointCommandState =>

  def setFilteredPoints(result: FilterMonitoringPointsCommandResult): Unit = {
    filteredPoints = result.pointMap
    studentDatas = result.studentDatas
  }
}

trait PopulateRecordMonitoringPointCommand extends PopulateOnForm {

  self: RecordMonitoringPointCommandState =>

  override def populate(): Unit = {
    val studentPointStateTuples: Seq[(StudentMember, AttendanceMonitoringPoint, AttendanceState)] =
      studentMap.flatMap { case (point, students) =>
        students.map(student => (student, point, {
          val pointMapOption = studentPointCheckpointMap.get(student)
          val checkpointOption = pointMapOption.flatMap { pointMap => pointMap.get(point) }
          val stateOption = checkpointOption.map { checkpoint => checkpoint.state }
          stateOption.orNull
        }))
      }.toSeq
    checkpointMap = studentPointStateTuples.groupBy(_._1).view.mapValues(
      _.groupBy(_._2).view.mapValues(_.head._3).toMap.asJava
    ).toMap.asJava
  }
}

trait RecordMonitoringPointValidation extends SelfValidating with GroupedPointRecordValidation {

  self: RecordMonitoringPointCommandState with AttendanceMonitoringServiceComponent with SecurityServiceComponent =>

  override def validate(errors: Errors): Unit = {
    validateGroupedPoint(
      errors,
      templatePoint,
      checkpointMap.asScala.view.mapValues(_.asScala.toMap).toMap,
      studentPointCheckpointMap.view.mapValues(_.view.mapValues(Option(_).map(_.state).orNull).toMap).toMap,
      user
    )
  }

}

trait RecordMonitoringPointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: RecordMonitoringPointCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MonitoringPoints.Record, department)
  }

}

trait RecordMonitoringPointDescription extends Describable[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

  self: RecordMonitoringPointCommandState =>

  override lazy val eventName = "RecordMonitoringPoint"

  override def describe(d: Description): Unit =
    d.attendanceMonitoringCheckpoints(checkpointMap.asScala.view.mapValues(_.asScala.toMap).toMap, verbose = true)
}

trait RecordMonitoringPointCommandState {

  self: AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

  def department: Department

  def academicYear: AcademicYear

  def templatePoint: AttendanceMonitoringPoint

  def user: CurrentUser

  var filteredPoints: Map[String, Seq[GroupedPoint]] = _
  var studentDatas: Seq[AttendanceMonitoringStudentData] = _

  lazy val pointsToRecord: Seq[AttendanceMonitoringPoint] = filteredPoints.values.flatten
    .find(p => p.templatePoint.id == templatePoint.id)
    .getOrElse(throw new IllegalArgumentException)
    .points

  lazy val studentMap: Map[AttendanceMonitoringPoint, Seq[StudentMember]] =
    pointsToRecord.map { point =>
      point -> profileService.getAllMembersWithUniversityIds(
        point.scheme.members.members
          .filter(universityId => studentDatas.exists(universityId == _.universityId))
          .toSeq
      ).flatMap {
        case student: StudentMember => Option(student)
        case _ => None
      }
    }.toMap

  lazy val studentPointCheckpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]] =
    attendanceMonitoringService.getCheckpoints(pointsToRecord, studentMap.values.flatten.toSeq.distinct)

  lazy val attendanceNoteMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringNote]] =
    studentMap.flatMap(_._2).map(student => student -> attendanceMonitoringService.getAttendanceNoteMap(student)).toMap

  lazy val hasReportedMap: Map[StudentMember, Boolean] =
    studentMap.flatMap(_._2).map(student =>
      student -> {
        val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(student), academicYear)
        !nonReportedTerms.contains(AcademicYear.forDate(templatePoint.startDate).termOrVacationForDate(templatePoint.startDate).periodType.toString)
      }
    ).toMap

  // Bind variables
  var checkpointMap: JMap[StudentMember, JMap[AttendanceMonitoringPoint, AttendanceState]] =
    LazyMaps.create { student: StudentMember => JHashMap(): JMap[AttendanceMonitoringPoint, AttendanceState] }.asJava
}
