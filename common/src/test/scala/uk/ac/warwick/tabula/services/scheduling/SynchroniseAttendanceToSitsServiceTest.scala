package uk.ac.warwick.tabula.services.scheduling

import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.After
import org.springframework.jdbc.core.{JdbcTemplate => JavaJdbcTemplate}
import org.springframework.jdbc.datasource.embedded.{EmbeddedDatabase, EmbeddedDatabaseBuilder}
import org.springframework.scala.jdbc.core.JdbcTemplate
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme, AttendanceState}
import uk.ac.warwick.tabula._

class SynchroniseAttendanceToSitsServiceTest extends TestBase with Mockito {

  val sits: EmbeddedDatabase = new EmbeddedDatabaseBuilder().addScript("sits-student-absence.sql").build()
  val jdbcTemplate: JdbcTemplate = new JdbcTemplate(new JavaJdbcTemplate(sits))

  val userLookup = new MockUserLookup()

  val service = new SynchroniseAttendanceToSitsServiceImpl
  service.sitsDataSource = sits
  service.userLookup = userLookup
  SynchroniseAttendanceToSitsService.sitsSchema = "public"
  service.afterPropertiesSet()

  val scheme = new AttendanceMonitoringScheme
  scheme.academicYear = AcademicYear.starting(2018)
  scheme.department = Fixtures.department("its")

  val point: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme)

  val student: StudentMember = Fixtures.student("1324597", courseDepartment = Fixtures.department("cs"))
  student.mostSignificantCourse.course = Fixtures.course("UCAS-R500")
  student.mostSignificantCourse.latestStudentCourseYearDetails.academicYear = scheme.academicYear

  val now: DateTime = new DateTime(2018, DateTimeConstants.SEPTEMBER, 19, 15, 39, 11, 293)

  @After def destroy(): Unit = {
    sits.shutdown()
  }

  private def inFixture(fn: => Unit): Unit =
    withUser("cuscav", "0672089") {
      currentUser.apparentUser.setDepartmentCode("IN")
      userLookup.registerUserObjects(currentUser.apparentUser)

      withFakeTime(now) {
        fn
      }
    }

  @Test def unauthorised(): Unit = inFixture {
    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    // Introspect the database to ensure that there's a single row with the right info
    jdbcTemplate.queryForObject[Int]("select count(*) from srs_sab") should be (Some(1))

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row should be (Map(
      "SAB_STUC" -> "1324597",
      "SAB_SEQ2" -> "001",
      "SAB_SRTN" -> null,
      "SAB_RAAC" -> "UNAUTH",
      "SAB_BEGD" -> null,
      "SAB_EXRD" -> null,
      "SAB_ENDD" -> java.sql.Date.valueOf("2018-09-19"),
      "SAB_PDAR" -> null,
      "SAB_AYRC" -> "18/19",
      "SAB_UDF1" -> null,
      "SAB_UDF2" -> "CS",
      "SAB_UDF3" -> "UCAS-R500",
      "SAB_UDF4" -> "IN0672089",
      "SAB_UDF5" -> null,
      "SAB_UDF6" -> null,
      "SAB_UDF7" -> "20180919T153911",
      "SAB_UDF8" -> null,
      "SAB_UDF9" -> "Tabula",
      "SAB_UDFA" -> null,
      "SAB_UDFB" -> null,
      "SAB_UDFC" -> null,
      "SAB_UDFD" -> null,
      "SAB_UDFE" -> null,
      "SAB_UDFF" -> null,
      "SAB_UDFG" -> null,
      "SAB_UDFH" -> null,
      "SAB_UDFI" -> null,
      "SAB_UDFJ" -> null,
      "SAB_UDFK" -> "id1",
      "SAB_NOTE" -> null
    ))
  }

  @Test def noOpDelete(): Unit = inFixture {
    jdbcTemplate.queryForObject[Int]("select count(*) from srs_sab") should be (Some(0))

    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.Attended)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    jdbcTemplate.queryForObject[Int]("select count(*) from srs_sab") should be (Some(0))
  }

  @Test def sequenceIsIncremented(): Unit = inFixture {
    val checkpoint1 = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint1.id = "id1"
    checkpoint1.updatedDate = DateTime.now.minusDays(1)
    checkpoint1.updatedBy = currentUser.userId

    val checkpoint2 = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint2.id = "id2"
    checkpoint2.updatedDate = DateTime.now
    checkpoint2.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint1) should be (true)
    service.synchroniseToSits(checkpoint2) should be (true)

    jdbcTemplate.queryForObject[Int]("select count(*) from srs_sab") should be (Some(2))
    jdbcTemplate.queryForObject[String]("select max(sab_seq2) from srs_sab") should be (Some("002"))
  }

  @Test def authorisedAfterUnauthorised(): Unit = inFixture {
    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now.minusDays(1)
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    jdbcTemplate.queryForObject[Int]("select count(*) from srs_sab") should be (Some(1))

    checkpoint.state = AttendanceState.MissedAuthorised
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    jdbcTemplate.queryForObject[Int]("select count(*) from srs_sab") should be (Some(0))
  }

  // Because we delete rows as the first thing, calling synchronise multiple times should still end up with a single row
  @Test def duplicateCallsDontCauseDuplicateRows(): Unit = inFixture {
    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now.minusDays(1)
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)
    service.synchroniseToSits(checkpoint) should be (true)
    service.synchroniseToSits(checkpoint) should be (true)
    service.synchroniseToSits(checkpoint) should be (true)
    service.synchroniseToSits(checkpoint) should be (true)

    jdbcTemplate.queryForObject[Int]("select count(*) from srs_sab") should be (Some(1))

    // Shouldn't increment the sequence unnecessarily
    jdbcTemplate.queryForObject[String]("select max(sab_seq2) from srs_sab") should be (Some("001"))
  }

  @Test def handlesNotFoundUsercode(): Unit = inFixture {
    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = "my-long-ass-extuser-name-that-will-get-truncated"

    service.synchroniseToSits(checkpoint) should be (true)

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row("SAB_UDF4") should be ("my-long-ass-ext")
  }

  @Test def handlesUsercodeWithNoUniversityId(): Unit = inFixture {
    currentUser.apparentUser.setWarwickId(null)

    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row("SAB_UDF4") should be ("cuscav")
  }

  @Test def handlesUsercodeWithNoDepartmentCode(): Unit = inFixture {
    currentUser.apparentUser.setDepartmentCode(null)

    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row("SAB_UDF4") should be ("cuscav")
  }

  @Test def handlesMissingCourse(): Unit = inFixture {
    student.mostSignificantCourse.course = null

    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row("SAB_UDF2") should be ("CS")
    row("SAB_UDF3") should be ("")
  }

  @Test def handlesMissingMostSignificantSCD(): Unit = inFixture {
    student.mostSignificantCourse = null

    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row("SAB_UDF2") should be ("")
    row("SAB_UDF3") should be ("")
  }

  @Test def handlesMissingEnrolmentDepartment(): Unit = inFixture {
    student.mostSignificantCourse.latestStudentCourseYearDetails.enrolmentDepartment = null

    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row("SAB_UDF2") should be ("")
    row("SAB_UDF3") should be ("UCAS-R500")
  }

  @Test def handlesMissingSCYD(): Unit = inFixture {
    student.mostSignificantCourse.removeStudentCourseYearDetails(student.mostSignificantCourse.latestStudentCourseYearDetails)

    val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point, student, AttendanceState.MissedUnauthorised)
    checkpoint.id = "id1"
    checkpoint.updatedDate = DateTime.now
    checkpoint.updatedBy = currentUser.userId

    service.synchroniseToSits(checkpoint) should be (true)

    val row = jdbcTemplate.queryForMap("select * from srs_sab where sab_stuc = ? and sab_seq2 = ?", "1324597", "001")
    row("SAB_UDF2") should be ("")
    row("SAB_UDF3") should be ("UCAS-R500")
  }
}
