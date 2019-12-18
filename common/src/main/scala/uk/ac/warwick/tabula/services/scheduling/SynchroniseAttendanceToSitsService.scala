package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.{MappingSqlQuery, SqlUpdate}
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JHashMap
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceState}
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.scheduling.SynchroniseAttendanceToSitsService.{SynchroniseAttendanceToSitsDeleteQuery, _}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

trait SynchroniseAttendanceToSitsServiceComponent {
  def synchroniseAttendanceToSitsService: SynchroniseAttendanceToSitsService
}

trait AutowiringSynchroniseAttendanceToSitsServiceComponent extends SynchroniseAttendanceToSitsServiceComponent {
  var synchroniseAttendanceToSitsService: SynchroniseAttendanceToSitsService = Wire[SynchroniseAttendanceToSitsService]
}

trait SynchroniseAttendanceToSitsService {
  def synchroniseToSits(checkpoint: AttendanceMonitoringCheckpoint): Boolean
  def synchroniseToSits(student: StudentMember, academicYear: AcademicYear, missedPoints: Int, updatedBy: User): Boolean
}

abstract class AbstractSynchroniseAttendanceToSitsService extends SynchroniseAttendanceToSitsService with InitializingBean {
  self: SitsDataSourceComponent
    with UserLookupComponent =>

  private[this] var existingRowQuery: SynchroniseAttendanceToSitsExistingRowQuery = _
  private[this] var updateNotMissedQuery: SynchroniseAttendanceToSitsUpdateNotMissedQuery = _
  private[this] var sequenceQuery: SynchroniseAttendanceToSitsCountQuery = _
  private[this] var insertQuery: SynchroniseAttendanceToSitsInsertQuery = _
  private[this] var deleteQuery: SynchroniseAttendanceToSitsDeleteQuery = _

  private[this] var getRowsQuery: SynchroniseAttendanceToSitsGetRowsQuery = _
  private[this] var updateNotMissedBySequenceQuery: SynchroniseAttendanceToSitsUpdateNotMissedBySequenceQuery = _
  private[this] var deleteBySequenceQuery: SynchroniseAttendanceToSitsDeleteBySequenceQuery = _

  override def afterPropertiesSet(): Unit = {
    existingRowQuery = new SynchroniseAttendanceToSitsExistingRowQuery(sitsDataSource)
    updateNotMissedQuery = new SynchroniseAttendanceToSitsUpdateNotMissedQuery(sitsDataSource)
    sequenceQuery = new SynchroniseAttendanceToSitsCountQuery(sitsDataSource)
    insertQuery = new SynchroniseAttendanceToSitsInsertQuery(sitsDataSource)
    deleteQuery = new SynchroniseAttendanceToSitsDeleteQuery(sitsDataSource)

    getRowsQuery = new SynchroniseAttendanceToSitsGetRowsQuery(sitsDataSource)
    updateNotMissedBySequenceQuery = new SynchroniseAttendanceToSitsUpdateNotMissedBySequenceQuery(sitsDataSource)
    deleteBySequenceQuery = new SynchroniseAttendanceToSitsDeleteBySequenceQuery(sitsDataSource)
  }

  override def synchroniseToSits(checkpoint: AttendanceMonitoringCheckpoint): Boolean = {
    val hasExistingMissedPoint: Option[Boolean] = existingRowQuery.executeByNamedParam(JHashMap(
      "universityId" -> checkpoint.student.universityId,
      "checkpointId" -> checkpoint.id,
    )).asScala.headOption

    checkpoint.state match {
      // Point is missed, there's an existing row
      case AttendanceState.MissedUnauthorised if hasExistingMissedPoint.contains(true) =>
        // Nothing to do here
        true

      // Point is missed
      case AttendanceState.MissedUnauthorised =>
        // Delete any existing row for this checkpoint
        val deletedRows = deleteQuery.updateByNamedParam(JHashMap(
          "universityId" -> checkpoint.student.universityId,
          "checkpointId" -> checkpoint.id,
        ))

        // Deleting 1 or 0 rows is fine
        if (deletedRows > 1) {
          throw new IllegalStateException(s"Expected to delete 1 or 0 rows for ${checkpoint.id}, but was actually $deletedRows")
        }

        val nextSequence = sequenceQuery.executeByNamedParam(JHashMap("universityId" -> checkpoint.student.universityId)).get(0) + 1

        val academicYear = checkpoint.point.scheme.academicYear

        val departmentCode =
          Option(checkpoint.student.mostSignificantCourse)
            .flatMap(_.freshStudentCourseYearDetailsForYear(academicYear))
            .flatMap(scyd => Option(scyd.enrolmentDepartment))
            .fold("")(_.code.toUpperCase)

        val courseCode =
          Option(checkpoint.student.mostSignificantCourse)
            .flatMap(scd => Option(scd.course))
            .fold("")(_.code.toUpperCase)

        val updatedByPrsCode =
          userLookup.getUserByUserId(checkpoint.updatedBy) match {
            case FoundUser(u) if u.getDepartmentCode.hasText && u.getWarwickId.hasText => s"${u.getDepartmentCode.toUpperCase}${u.getWarwickId}"
            case _ => checkpoint.updatedBy
          }

        insertQuery.updateByNamedParam(JHashMap(
          "universityId" -> checkpoint.student.universityId,
          "sequence" -> f"$nextSequence%03d",
          "updatedDate" -> new java.sql.Timestamp(checkpoint.updatedDate.getMillis),
          "academicYear" -> academicYear.toString,
          "departmentCode" -> departmentCode,
          "courseCode" -> courseCode,
          "updatedBy" -> updatedByPrsCode.take(SynchroniseAttendanceToSitsService.recorderMaxColumnSize),
          "uploadedDateTime" -> DateTime.now.toString(SynchroniseAttendanceToSitsService.uploadedDateTimeFormat),
          "checkpointId" -> checkpoint.id,
        )) == 1

      // Point is not missed, there's an existing row
      case _ if hasExistingMissedPoint.contains(true) =>
        // Update the existing row to have 0 missed points
        updateNotMissedQuery.updateByNamedParam(JHashMap(
          "universityId" -> checkpoint.student.universityId,
          "checkpointId" -> checkpoint.id,
        )) == 1

      case _ => true
    }
  }

  override def synchroniseToSits(student: StudentMember, academicYear: AcademicYear, missedPoints: Int, updatedBy: User): Boolean = {
    val existingRows: Seq[StudentAbsenceRow] = getRowsQuery.executeByNamedParam(JHashMap(
      "universityId" -> student.universityId,
      "academicYear" -> academicYear.toString,
    )).asScala.toSeq

    val existingMissedPoints: Int = existingRows.map(_.absences).sum

    if (missedPoints == existingMissedPoints) true // Matching values
    else if (missedPoints > existingMissedPoints) {
      // Need to add more rows
      val expectedInsertedRows = missedPoints - existingMissedPoints

      val departmentCode =
          Option(student.mostSignificantCourse)
            .flatMap(_.freshStudentCourseYearDetailsForYear(academicYear))
            .flatMap(scyd => Option(scyd.enrolmentDepartment))
            .fold("")(_.code.toUpperCase)

        val courseCode =
          Option(student.mostSignificantCourse)
            .flatMap(scd => Option(scd.course))
            .fold("")(_.code.toUpperCase)

        val updatedByPrsCode =
          updatedBy match {
            case FoundUser(u) if u.getDepartmentCode.hasText && u.getWarwickId.hasText => s"${u.getDepartmentCode.toUpperCase}${u.getWarwickId}"
            case _ => updatedBy.getUserId
          }

      (existingMissedPoints until missedPoints).map { _ =>
        val nextSequence = sequenceQuery.executeByNamedParam(JHashMap("universityId" -> student.universityId)).get(0) + 1

        insertQuery.updateByNamedParam(JHashMap(
          "universityId" -> student.universityId,
          "sequence" -> f"$nextSequence%03d",
          "updatedDate" -> new java.sql.Timestamp(DateTime.now.getMillis),
          "academicYear" -> academicYear.toString,
          "departmentCode" -> departmentCode,
          "courseCode" -> courseCode,
          "updatedBy" -> updatedByPrsCode.take(SynchroniseAttendanceToSitsService.recorderMaxColumnSize),
          "uploadedDateTime" -> DateTime.now.toString(SynchroniseAttendanceToSitsService.uploadedDateTimeFormat),
          "checkpointId" -> null,
        ))
      }.sum == expectedInsertedRows
    } else {
      // Need to update some rows
      val expectedUpdatedRows = existingMissedPoints - missedPoints
      existingRows.filter(_.absences == 1).takeRight(expectedUpdatedRows).map { row =>
        updateNotMissedBySequenceQuery.updateByNamedParam(JHashMap(
          "universityId" -> row.universityId,
          "sequence" -> row.sequence,
        ))
      }.sum == expectedUpdatedRows
    }
  }
}

/**
 * SQL fields we are interested in/write to:
 *
 * - SAB_STUC 7-digit UniversityId (PK, indexed)
 * - SAB_SEQ2 Unique sequence number, positive integer padded with zeroes to 3 characters (%03d) (PK)
 * - SAB_RAAC Reason for absence code - we always write UNAUTH
 * - SAB_ENDD The updated date for the AttendanceMonitoringCheckpoint
 * - SAB_AYRC Academic year code yy/zz
 * - SAB_UDF2 Enrolment department code that is monitoring the student
 * - SAB_UDF3 Course code that the student is taking while being monitored
 * - SAB_UDF4 SITS PRS code of the person who has reported the absence
 * - SAB_UDF5 Number of missed points - inserted as 1, updated to 0 on a "deletion"
 * - SAB_UDF7 Date of synchronisation, formatted as yyyyMMdd'T'HHmmss to fit into 15 characters
 * - SAB_UDF9 System of origin; always 'Tabula'
 * - SAB_UDFK AttendanceMonitoringCheckpoint UUID, for updates
 */
object SynchroniseAttendanceToSitsService {
  var sitsSchema: String = Wire.property("${schema.sits}")
  val recorderMaxColumnSize = 15
  val uploadedDateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss")

  // find the latest row in the Student Absence (SAB) table in SITS for the student - PK is SAB_STUC, SAB_SEQ2
  final def GetHighestExistingSequenceSql =
    s"""
      select max(SAB_SEQ2) as max_sequence from $sitsSchema.srs_sab
      where SAB_STUC = :universityId
    """

  final def GetExistingRowSql =
    s"""
      select SAB_UDF5 as missed_point_count from $sitsSchema.srs_sab
      where SAB_STUC = :universityId and SAB_UDFK = :checkpointId
    """

  // insert into Student Absence (SAB) table
  final def PushToSITSSql =
    s"""
      insert into $sitsSchema.srs_sab
      (SAB_STUC,SAB_SEQ2,SAB_RAAC,SAB_ENDD,SAB_AYRC,SAB_UDF2,SAB_UDF3,SAB_UDF4,SAB_UDF5,SAB_UDF7,SAB_UDF9,SAB_UDFK)
      values (:universityId, :sequence, 'UNAUTH', :updatedDate, :academicYear, :departmentCode, :courseCode, :updatedBy, 1, :uploadedDateTime, 'Tabula', :checkpointId)
    """

  // update the existing row in the Student Absence (SAB) table
  final def UpdateSITSSetNotMissedSql =
    s"""
      update $sitsSchema.srs_sab
      set SAB_UDF5 = 0
      where SAB_STUC = :universityId and SAB_UDFK = :checkpointId
    """

  // delete the existing row in the Student Absence (SAB) table
  final def DeleteFromSITSSql =
    s"""
      delete from $sitsSchema.srs_sab
      where SAB_STUC = :universityId and SAB_UDFK = :checkpointId
    """

  class SynchroniseAttendanceToSitsCountQuery(ds: DataSource) extends MappingSqlQuery[Int](ds, GetHighestExistingSequenceSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): Int = rs.getInt("max_sequence")
  }

  class SynchroniseAttendanceToSitsExistingRowQuery(ds: DataSource) extends MappingSqlQuery[Boolean](ds, GetExistingRowSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("checkpointId", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): Boolean = rs.getInt("missed_point_count") == 1
  }

  class SynchroniseAttendanceToSitsInsertQuery(ds: DataSource) extends SqlUpdate(ds, PushToSITSSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    declareParameter(new SqlParameter("updatedDate", Types.TIMESTAMP))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("departmentCode", Types.VARCHAR))
    declareParameter(new SqlParameter("courseCode", Types.VARCHAR))
    declareParameter(new SqlParameter("updatedBy", Types.VARCHAR))
    declareParameter(new SqlParameter("uploadedDateTime", Types.VARCHAR))
    declareParameter(new SqlParameter("checkpointId", Types.VARCHAR))
    compile()
  }

  class SynchroniseAttendanceToSitsUpdateNotMissedQuery(ds: DataSource) extends SqlUpdate(ds, UpdateSITSSetNotMissedSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("checkpointId", Types.VARCHAR))
    compile()
  }

  class SynchroniseAttendanceToSitsDeleteQuery(ds: DataSource) extends SqlUpdate(ds, DeleteFromSITSSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("checkpointId", Types.VARCHAR))
    compile()
  }

  case class StudentAbsenceRow(universityId: String, sequence: String, absences: Int)

  final def GetRowsQuerySql =
    s"""
      select
        SAB_STUC as university_id,
        SAB_SEQ2 as sequence_no,
        SAB_UDF5 as missed_point_count
      from $sitsSchema.srs_sab
      where SAB_STUC = :universityId and SAB_AYRC = :academicYear and SAB_UDFK is null
      order by SAB_STUC, SAB_SEQ2
    """

  class SynchroniseAttendanceToSitsGetRowsQuery(ds: DataSource) extends MappingSqlQuery[StudentAbsenceRow](ds, GetRowsQuerySql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): StudentAbsenceRow =
      StudentAbsenceRow(
        rs.getString("university_id"),
        rs.getString("sequence_no"),
        rs.getInt("missed_point_count")
      )
  }

  // update the existing row in the Student Absence (SAB) table
  final def UpdateSITSSetNotMissedBySequenceSql =
    s"""
      update $sitsSchema.srs_sab
      set SAB_UDF5 = 0
      where SAB_STUC = :universityId and SAB_SEQ2 = :sequence
    """

  class SynchroniseAttendanceToSitsUpdateNotMissedBySequenceQuery(ds: DataSource) extends SqlUpdate(ds, UpdateSITSSetNotMissedBySequenceSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    compile()
  }

  // delete the existing row in the Student Absence (SAB) table
  final def DeleteFromSITSBySequenceSql =
    s"""
      delete from $sitsSchema.srs_sab
      where SAB_STUC = :universityId and SAB_UDFK = :checkpointId
    """

  class SynchroniseAttendanceToSitsDeleteBySequenceQuery(ds: DataSource) extends SqlUpdate(ds, DeleteFromSITSBySequenceSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    compile()
  }
}

@Profile(Array("dev", "test", "production"))
@Service
class SynchroniseAttendanceToSitsServiceImpl
  extends AbstractSynchroniseAttendanceToSitsService
    with AutowiringSitsDataSourceComponent
    with AutowiringUserLookupComponent

@Profile(Array("sandbox"))
@Service
class SynchroniseAttendanceToSitsSandboxService extends SynchroniseAttendanceToSitsService {
  override def synchroniseToSits(checkpoint: AttendanceMonitoringCheckpoint) = false
  override def synchroniseToSits(student: StudentMember, academicYear: AcademicYear, missedPoints: Int, updatedBy: User): Boolean = false
}
