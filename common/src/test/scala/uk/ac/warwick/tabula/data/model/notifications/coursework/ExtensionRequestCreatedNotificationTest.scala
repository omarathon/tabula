package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Notification, StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, MarkdownRendererImpl, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ExtensionRequestCreatedNotificationTest extends TestBase with ExtensionNotificationTesting with Mockito with FreemarkerRendering with MarkdownRendererImpl {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  def createNotification(extension: Extension, student: User, studentMember: Option[StudentMember] = None): ExtensionRequestCreatedNotification = {
    val n = Notification.init(new ExtensionRequestCreatedNotification, student, Seq(extension), extension.assignment)
    n.userLookup = mockUserLookup
    n.profileService = mockProfileService
    n.relationshipService = mockRelationshipService

    wireUserlookup(n, student)
    n.profileService.getMemberByUniversityId(student.getWarwickId) returns studentMember
    studentMember.flatMap(_.mostSignificantCourseDetails).foreach { scd =>
      val tutorType = StudentRelationshipType("tutor", "tutor", "tutor", "tutor")
      tutorType.description = "Personal tutor"

      val supervisorType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisor")
      supervisorType.description = "Research supervisor"

      val tutor1 = Fixtures.staff("0000001")
      tutor1.jobTitle = "Senior tutor"
      tutor1.homeDepartment = Fixtures.department("cs", "Computer Science")

      val tutor1Rel = StudentRelationship(tutor1, tutorType, scd, DateTime.now)

      val tutor2 = Fixtures.staff("0000002")
      tutor2.jobTitle = "Academic partner"
      tutor2.homeDepartment = Fixtures.department("cs", "Computer Science")

      val tutor2Rel = StudentRelationship(tutor2, tutorType, scd, DateTime.now)

      val supervisor = Fixtures.staff("0000003")
      supervisor.jobTitle = "Professor"
      supervisor.homeDepartment = Fixtures.department("ts", "Tabula Studies")

      val supervisorRel = StudentRelationship(supervisor, supervisorType, scd, DateTime.now)

      n.relationshipService.allStudentRelationshipTypes returns Seq(tutorType, supervisorType)
      n.relationshipService.findCurrentRelationships(tutorType, scd) returns Seq(tutor1Rel, tutor2Rel)
      n.relationshipService.findCurrentRelationships(supervisorType, scd) returns Seq(supervisorRel)
    }

    n
  }

  @Test
  def urlIsAssignmentExtensionsPage(): Unit = new ExtensionFixture {
    val n: ExtensionRequestCreatedNotification = createNotification(extension, student)
    n.url should be(s"/$cm2Prefix/admin/assignments/123/extensions?usercode=u1234567")
  }

  @Test
  def titleShouldContainMessage(): Unit = new ExtensionFixture {
    val n: ExtensionRequestCreatedNotification = createNotification(extension, student)
    n.title.contains("New extension request made") should be(true)
  }


  @Test
  def recipientsContainsAllAdmins(): Unit = new ExtensionFixture {
    val n: ExtensionRequestCreatedNotification = createNotification(extension, student)
    n.recipients should be(Seq(admin, admin2, admin3))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = new ExtensionFixture {
    val n: ExtensionRequestCreatedNotification = createNotification(extension, student)
    n.content.template should be("/WEB-INF/freemarker/emails/new_extension_request.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new ExtensionFixture {
    val n: ExtensionRequestCreatedNotification = createNotification(extension, student)
    n.content.model("requestedExpiryDate") should be("23 August 2013 at 12:00:00")
    n.content.model.get("reasonForRequest") should be('empty)
    n.url should be(s"/$cm2Prefix/admin/assignments/123/extensions?usercode=" + student.getUserId)
    n.content.model("assignment") should be(assignment)
    n.content.model("student") should be(student)
  }

  @Test
  def title() {
    new ExtensionFixture {
      module.code = "cs118"
      assignment.name = "5,000 word essay"
      student.setFullName("John Studentson")

      val n: ExtensionRequestCreatedNotification = createNotification(extension, student)
      n.title should be("CS118: New extension request made by John Studentson for \"5,000 word essay\"")
    }
  }

  @Test
  def textIsFormattedCorrectly(): Unit = new ExtensionFixture {
    val n: ExtensionRequestCreatedNotification = createNotification(extension, student, Some(studentMember))

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """John Smith (1234657) has requested an extension for the assignment 'Essay' for XXX Module.
        |
        |They have requested an extension until 23 August 2013 at 12:00:00.
        |
        |Further details related to this request:
        |
        |Module Managers:
        |
        |* Manager1y McManager1erson (3266596) (manager1@example.com)
        |* Manager2y McManager2erson (3266597) (manager2@example.com)
        |
        |Student contact details:
        |
        |* Mobile number: Not available
        |* Telephone number: Not available
        |* Email address: J.Smith@tabula.warwick.ac.uk
        |
        |Personal tutors:
        |
        |* 0000001 Staff (0000001), Senior tutor, Computer Science
        |* 0000002 Staff (0000002), Academic partner, Computer Science
        |
        |Research supervisor:
        |
        |* 0000003 Staff (0000003), Professor, Tabula Studies
        |
        |Student course details:
        |
        |* Route: Tabula Studies (BEng) (T100)
        |* Course: Tabula Studies (UTAB-T100)
        |* Intended award and type: BEng (Undergraduate)
        |""".stripMargin
    )

    // Make sure the Markdown -> HTML is sane
    renderMarkdown(notificationContent) should be (
      """<p>John Smith (1234657) has requested an extension for the assignment &#39;Essay&#39; for XXX Module.</p>
        |<p>They have requested an extension until 23 August 2013 at 12:00:00.</p>
        |<p>Further details related to this request:</p>
        |<p>Module Managers:</p>
        |<ul><li>Manager1y McManager1erson (3266596) (manager1&#64;example.com)</li><li>Manager2y McManager2erson (3266597) (manager2&#64;example.com)</li></ul>
        |<p>Student contact details:</p>
        |<ul><li>Mobile number: Not available</li><li>Telephone number: Not available</li><li>Email address: J.Smith&#64;tabula.warwick.ac.uk</li></ul>
        |<p>Personal tutors:</p>
        |<ul><li>0000001 Staff (0000001), Senior tutor, Computer Science</li><li>0000002 Staff (0000002), Academic partner, Computer Science</li></ul>
        |<p>Research supervisor:</p>
        |<ul><li>0000003 Staff (0000003), Professor, Tabula Studies</li></ul>
        |<p>Student course details:</p>
        |<ul><li>Route: Tabula Studies (BEng) (T100)</li><li>Course: Tabula Studies (UTAB-T100)</li><li>Intended award and type: BEng (Undergraduate)</li></ul>
        |""".stripMargin
    )
  }

  @Test
  def textIsFormattedCorrectlyWithEmptySections(): Unit = new ExtensionFixture {
    module.managers.includedUserIds = Set.empty

    val n: ExtensionRequestCreatedNotification = createNotification(extension, student, None)

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """John Smith (1234657) has requested an extension for the assignment 'Essay' for XXX Module.
        |
        |They have requested an extension until 23 August 2013 at 12:00:00.
        |
        |Further details related to this request:
        |
        |Student contact details:
        |
        |* Mobile number: Not available
        |* Telephone number: Not available
        |* Email address: J.Smith@tabula.warwick.ac.uk
        |""".stripMargin
    )

    // Make sure the Markdown -> HTML is sane
    renderMarkdown(notificationContent) should be (
      """<p>John Smith (1234657) has requested an extension for the assignment &#39;Essay&#39; for XXX Module.</p>
        |<p>They have requested an extension until 23 August 2013 at 12:00:00.</p>
        |<p>Further details related to this request:</p>
        |<p>Student contact details:</p>
        |<ul><li>Mobile number: Not available</li><li>Telephone number: Not available</li><li>Email address: J.Smith&#64;tabula.warwick.ac.uk</li></ul>
        |""".stripMargin
    )
  }
}
