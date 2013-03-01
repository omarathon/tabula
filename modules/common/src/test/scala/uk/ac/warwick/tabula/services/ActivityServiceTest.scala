package uk.ac.warwick.tabula.services
import org.apache.lucene.search.FieldDoc
import org.hamcrest.BaseMatcher
import org.joda.time.DateTime
import org.specs.mock.JMocker.`with`
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Activity
import uk.ac.warwick.tabula.data.model.AuditEvent
import org.mockito.Matchers._

class ActivityServiceTest extends TestBase with Mockito {
	
	val service = new ActivityService
	
	val moduleService = mock[ModuleAndDepartmentService]
	val assignmentService = mock[AssignmentService]
	val auditIndexService = mock[AuditEventIndexService]
	val userLookup = new MockUserLookup
	userLookup.registerUsers("cuscav")
	
	Activity.assignmentService = assignmentService
	Activity.userLookup = userLookup
	
	service.moduleService = moduleService
	service.assignmentService = assignmentService
	service.auditIndexService = auditIndexService
	
	@Test def getNoteworthySubmissionsFirstPage = withUser("cuscav") {
		val om1 = Fixtures.module("own1")
		val om2 = Fixtures.module("own2")
		
		moduleService.modulesManagedBy("cuscav") returns (Seq(om1, om2))
		
		val am1 = Fixtures.module("admin1")
		val am2 = Fixtures.module("admin2")
		
		moduleService.modulesAdministratedBy("cuscav") returns (Seq(am1, am2, om1))
		
		val ae1 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId1"}"""
		)
		ae1.related = Seq(ae1)
		ae1.parsedData = Some(json.readValue(ae1.data, classOf[Map[String, Any]]))
		
		val ae2 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId2"}"""
		)
		ae2.related = Seq(ae2)
		ae2.parsedData = Some(json.readValue(ae2.data, classOf[Map[String, Any]]))
		
		val submission1 = Fixtures.submission()
		val submission2 = Fixtures.submission()
		
		assignmentService.getSubmission("submissionId1") returns (Some(submission1))
		assignmentService.getSubmission("submissionId2") returns (Some(submission2))
		
		val fieldDoc = new FieldDoc(100, 0.5f, Array())
		val pae = PagedAuditEvents(Seq(ae1, ae2), Some(fieldDoc), 20, 30)
		
		auditIndexService.noteworthySubmissionsForModules(Seq(om1, om2, am1, am2), None, None, 8) returns(pae)
		
		val activities = service.getNoteworthySubmissions(currentUser)
		activities.activities.size should be (2)
		activities.activities(0) should not be (null)
		activities.activities(0).title should be ("New submission")
		activities.activities(0).message should be ("")
		activities.activities(0).date should be (ae1.eventDate)
		activities.activities(0).agent.getUserId() should be ("cuscav")
		activities.activities(0).entity should be (submission1)
		
		activities.activities(1) should not be (null)
		activities.activities(1).title should be ("New submission")
		activities.activities(1).message should be ("")
		activities.activities(1).date should be (ae2.eventDate)
		activities.activities(1).agent.getUserId() should be ("cuscav")
		activities.activities(1).entity should be (submission2)
		
		activities.doc should be (Some(fieldDoc))
		activities.token should be (20)
		activities.total should be (30)
	}
	
	@Test def getNoteworthySubmissionsFollowingPages = withUser("cuscav") {
		val om1 = Fixtures.module("own1")
		val om2 = Fixtures.module("own2")
		
		moduleService.modulesManagedBy("cuscav") returns (Seq(om1, om2))
		
		val am1 = Fixtures.module("admin1")
		val am2 = Fixtures.module("admin2")
		
		moduleService.modulesAdministratedBy("cuscav") returns (Seq(am1, am2, om1))
		
		val ae1 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId1"}"""
		)
		ae1.related = Seq(ae1)
		ae1.parsedData = Some(json.readValue(ae1.data, classOf[Map[String, Any]]))
		
		val ae2 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId2"}"""
		)
		ae2.related = Seq(ae2)
		ae2.parsedData = Some(json.readValue(ae2.data, classOf[Map[String, Any]]))
		
		val submission1 = Fixtures.submission()
		val submission2 = Fixtures.submission()
		
		assignmentService.getSubmission("submissionId1") returns (Some(submission1))
		assignmentService.getSubmission("submissionId2") returns (Some(submission2))
		
		val fieldDoc = new FieldDoc(100, 0.5f, Array())
		val pae = PagedAuditEvents(Seq(ae1, ae2), Some(fieldDoc), 20, 30)
		
		auditIndexService.noteworthySubmissionsForModules(isEq(Seq(om1, om2, am1, am2)), isA(classOf[Option[FieldDoc]]), isEq(Some(30)), isEq(8)) returns(pae)
		
		val activities = service.getNoteworthySubmissions(currentUser, 10, 20, 30)
		activities.activities.size should be (2)
		activities.activities(0) should not be (null)
		activities.activities(0).title should be ("New submission")
		activities.activities(0).message should be ("")
		activities.activities(0).date should be (ae1.eventDate)
		activities.activities(0).agent.getUserId() should be ("cuscav")
		activities.activities(0).entity should be (submission1)
		
		activities.activities(1) should not be (null)
		activities.activities(1).title should be ("New submission")
		activities.activities(1).message should be ("")
		activities.activities(1).date should be (ae2.eventDate)
		activities.activities(1).agent.getUserId() should be ("cuscav")
		activities.activities(1).entity should be (submission2)
		
		activities.doc should be (Some(fieldDoc))
		activities.token should be (20)
		activities.total should be (30)
	}

}