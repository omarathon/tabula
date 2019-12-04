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
import uk.ac.warwick.tabula.JavaImports.JHashMap
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceState}
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.services.scheduling.SynchroniseAttendanceToSitsService.{SynchroniseAttendanceToSitsCountQuery, SynchroniseAttendanceToSitsDeleteQuery, SynchroniseAttendanceToSitsUpdateQuery}

trait SynchroniseAttendanceToSitsServiceComponent {
  def synchroniseAttendanceToSitsService: SynchroniseAttendanceToSitsService
}

trait AutowiringSynchroniseAttendanceToSitsServiceComponent extends SynchroniseAttendanceToSitsServiceComponent {
  var synchroniseAttendanceToSitsService: SynchroniseAttendanceToSitsService = Wire[SynchroniseAttendanceToSitsService]
}

trait SynchroniseAttendanceToSitsService {
  def synchroniseToSits(checkpoint: AttendanceMonitoringCheckpoint): Boolean
}

abstract class AbstractSynchroniseAttendanceToSitsService extends SynchroniseAttendanceToSitsService with InitializingBean {
  self: SitsDataSourceComponent
    with UserLookupComponent =>

  private[this] var deleteQuery: SynchroniseAttendanceToSitsDeleteQuery = _
  private[this] var sequenceQuery: SynchroniseAttendanceToSitsCountQuery = _
  private[this] var updateQuery: SynchroniseAttendanceToSitsUpdateQuery = _

  override def afterPropertiesSet(): Unit = {
    deleteQuery = new SynchroniseAttendanceToSitsDeleteQuery(sitsDataSource)
    sequenceQuery = new SynchroniseAttendanceToSitsCountQuery(sitsDataSource)
    updateQuery = new SynchroniseAttendanceToSitsUpdateQuery(sitsDataSource)
  }

  def synchroniseToSits(checkpoint: AttendanceMonitoringCheckpoint): Boolean = {
    // Delete any existing row for this checkpoint
    val deletedRows = deleteQuery.updateByNamedParam(JHashMap(
      "universityId" -> checkpoint.student.universityId,
      "checkpointId" -> checkpoint.id,
    ))

    // Deleting 1 or 0 rows is fine
    if (deletedRows > 1) {
      throw new IllegalStateException(s"Expected to delete 1 or 0 rows for ${checkpoint.id}, but was actually $deletedRows")
    }

    if (checkpoint.state != AttendanceState.MissedUnauthorised) true
    else {
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

      updateQuery.updateByNamedParam(JHashMap(
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
 * - SAB_UDF7 Date of synchronisation, formatted as yyyyMMdd'T'HHmmss to fit into 15 characters
 * - SAB_UDF9 System of origin; always 'Tabula'
 * - SAB_UDFK AttendanceMonitoringCheckpoint UUID, for updates
 */
object SynchroniseAttendanceToSitsService {
  var sitsSchema: String = Wire.property("${schema.sits}")
  val recorderMaxColumnSize = 15
  val uploadedDateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss")

  // find the latest row in the Student Absence (SAB) table in SITS for the student - PK is SAB_STUC, SAB_SEQ2
  final def GetHighestExistingSequence =
    f"""
		select max(sab_seq2) as max_sequence from $sitsSchema.srs_sab
		where sab_stuc = :universityId
	"""

  // insert into Student Absence (SAB) table
  final def PushToSITSSql =
    f"""
		insert into $sitsSchema.srs_sab
		(SAB_STUC,SAB_SEQ2,SAB_RAAC,SAB_ENDD,SAB_AYRC,SAB_UDF2,SAB_UDF3,SAB_UDF4,SAB_UDF7,SAB_UDF9,SAB_UDFK)
		values (:universityId, :sequence, 'UNAUTH', :updatedDate, :academicYear, :departmentCode, :courseCode, :updatedBy, :uploadedDateTime, 'Tabula', :checkpointId)
	"""

  // delete from Student Absence (SAB) table
  final def DeleteFromSITSSql =
    f"""
		delete from $sitsSchema.srs_sab
    where sab_stuc = :universityId and sab_udfk = :checkpointId
	"""

  class SynchroniseAttendanceToSitsCountQuery(ds: DataSource) extends MappingSqlQuery[Int](ds, GetHighestExistingSequence) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): Int = rs.getInt("max_sequence")
  }

  class SynchroniseAttendanceToSitsUpdateQuery(ds: DataSource) extends SqlUpdate(ds, PushToSITSSql) {
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

  class SynchroniseAttendanceToSitsDeleteQuery(ds: DataSource) extends SqlUpdate(ds, DeleteFromSITSSql) {
    declareParameter(new SqlParameter("universityId", Types.VARCHAR))
    declareParameter(new SqlParameter("checkpointId", Types.VARCHAR))
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
  def synchroniseToSits(checkpoint: AttendanceMonitoringCheckpoint) = false
}
