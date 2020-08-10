package uk.ac.warwick.tabula.data.model.notifications.attendance

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpointTotal
import uk.ac.warwick.tabula.helpers.FreemarkerMapHelper
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, ScopelessPermission}
import uk.ac.warwick.tabula.services.{RelationshipService, SecurityService}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaBeansWrapper, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula._

class MissedAttendanceMonitoringCheckpointsNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val securityService: SecurityService = mock[SecurityService]
  securityService.can(any[CurrentUser], any[ScopelessPermission]) returns true
  securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget]) returns true

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
  freeMarkerConfig.getObjectWrapper.asInstanceOf[ScalaBeansWrapper].securityService = securityService
  freeMarkerConfig.setSharedVariable("mapGet", new FreemarkerMapHelper)

  private trait Fixture {
    val department: Department = Fixtures.department("es", "Engineering")

    val agent: StaffMember = Fixtures.staff("1234567", "estaff", department)
    agent.firstName = "Karen"
    agent.lastName = "Bradbury"

    val student: StudentMember = Fixtures.student("1218503", "esustu", department)
    student.firstName = "Tardy"
    student.lastName = "Studentperson"

    val relationshipType: StudentRelationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
    val relationships = Seq(StudentRelationship(agent, relationshipType, student, DateTime.now))

    val relationshipService: RelationshipService = smartMock[RelationshipService]
    relationshipService.getAllCurrentRelationships(student) returns relationships

    val total: AttendanceMonitoringCheckpointTotal = Fixtures.attendanceMonitoringCheckpointTotal(student, department, AcademicYear.now())
  }

  @Test def low(): Unit = new Fixture {
    val notification: MissedAttendanceMonitoringCheckpointsLowNotification = Notification.init(new MissedAttendanceMonitoringCheckpointsLowNotification, null, total)
    notification.relationshipService = relationshipService

    notification.titleFor(agent.asSsoUser) should be ("Tardy Studentperson has missed 3 monitoring points")

    val content: FreemarkerModel = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """The following student has missed at least 3 monitoring points in the academic year 20/21:
        |
        |- Name: Tardy Studentperson
        |- University ID: 1218503
        |- Course: Course UCSA-G500
        |  - Personal tutor: Karen Bradbury
        |
        |Please refer to the Education Policy and Quality website for guidance on the appropriate action to take:
        |
        |http://warwick.ac.uk/studentattendanceguide/principles
        |""".stripMargin
    )
  }

  @Test def medium(): Unit = new Fixture {
    val notification: MissedAttendanceMonitoringCheckpointsMediumNotification = Notification.init(new MissedAttendanceMonitoringCheckpointsMediumNotification, null, total)
    notification.relationshipService = relationshipService

    notification.titleFor(agent.asSsoUser) should be ("Tardy Studentperson has missed 6 monitoring points")

    val content: FreemarkerModel = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """The following student has missed at least 6 monitoring points in the academic year 20/21:
        |
        |- Name: Tardy Studentperson
        |- University ID: 1218503
        |- Course: Course UCSA-G500
        |  - Personal tutor: Karen Bradbury
        |
        |Please refer to the Education Policy and Quality website for guidance on the appropriate action to take:
        |
        |http://warwick.ac.uk/studentattendanceguide/principles
        |""".stripMargin
    )
  }

  @Test def high(): Unit = new Fixture {
    val notification: MissedAttendanceMonitoringCheckpointsHighNotification = Notification.init(new MissedAttendanceMonitoringCheckpointsHighNotification, null, total)
    notification.relationshipService = relationshipService

    notification.titleFor(agent.asSsoUser) should be ("Tardy Studentperson has missed 8 monitoring points")

    val content: FreemarkerModel = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """The following student has missed at least 8 monitoring points in the academic year 20/21:
        |
        |- Name: Tardy Studentperson
        |- University ID: 1218503
        |- Course: Course UCSA-G500
        |  - Personal tutor: Karen Bradbury
        |
        |Please refer to the Education Policy and Quality website for guidance on the appropriate action to take:
        |
        |http://warwick.ac.uk/studentattendanceguide/principles
        |""".stripMargin
    )
  }

  @Test def batch(): Unit = new Fixture {
    val notification1: MissedAttendanceMonitoringCheckpointsLowNotification = Notification.init(new MissedAttendanceMonitoringCheckpointsLowNotification, null, total)
    val notification2: MissedAttendanceMonitoringCheckpointsLowNotification = Notification.init(new MissedAttendanceMonitoringCheckpointsLowNotification, null, total)
    val notification3: MissedAttendanceMonitoringCheckpointsLowNotification = Notification.init(new MissedAttendanceMonitoringCheckpointsLowNotification, null, total)

    val batch = Seq(notification1, notification2, notification3)

    batch.foreach(_.relationshipService = relationshipService)

    MissedAttendanceMonitoringCheckpointsBatchedNotificationHandler.titleForBatch(batch, agent.asSsoUser) should be ("3 students have missed 3 monitoring points")

    val content: FreemarkerModel = MissedAttendanceMonitoringCheckpointsBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """The following 3 students have missed at least 3 monitoring points in the academic year 20/21:
        |
        |- Tardy Studentperson (1218503) - Course UCSA-G500
        |- Tardy Studentperson (1218503) - Course UCSA-G500
        |- Tardy Studentperson (1218503) - Course UCSA-G500
        |
        |Please refer to the Education Policy and Quality website for guidance on the appropriate action to take:
        |
        |http://warwick.ac.uk/studentattendanceguide/principles
        |""".stripMargin
    )
  }

}
