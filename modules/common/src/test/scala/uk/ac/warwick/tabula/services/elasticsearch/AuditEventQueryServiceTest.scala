package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.testkit.{ElasticSugar, IndexMatchers, SearchMatchers}
import org.hibernate.dialect.HSQLDialect
import org.joda.time.DateTime
import org.junit.{After, Before}
import org.scalatest.time.{Millis, Seconds, Span}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.commands.{Description, NullCommand}
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.services.AuditEventServiceImpl
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, PersistenceTestBase}
import uk.ac.warwick.util.core.StopWatch

import scala.collection.JavaConverters._

class AuditEventQueryServiceTest extends PersistenceTestBase with Mockito with ElasticSugar with IndexMatchers with SearchMatchers {

	override implicit val patienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	val indexName = "audit"
	val indexType = new AuditEventIndexType {}.indexType

	private trait Fixture {
		val service: AuditEventServiceImpl = new AuditEventServiceImpl with SessionComponent {
			val session = sessionFactory.getCurrentSession
		}
		service.dialect = new HSQLDialect()

		val queryService = new AuditEventQueryServiceImpl
		queryService.userLookup = new MockUserLookup()
		queryService.auditEventService = service
		queryService.client = AuditEventQueryServiceTest.this.client
		queryService.indexName = AuditEventQueryServiceTest.this.indexName

		implicit val indexable = AuditEventIndexService.auditEventIndexable(service)
	}

	@Before def setUp(): Unit = {
		new AuditEventElasticsearchConfig {
			client.execute {
				create index indexName mappings (mapping(indexType) fields fields) analysis analysers
			}.await.isAcknowledged should be(true)
		}
		blockUntilIndexExists(indexName)
	}

	@After def tearDown(): Unit = {
		client.execute { delete index indexName }
		blockUntilIndexNotExists(indexName)
	}

	/**
		* Check that when you download submissions, they are shown
		* by adminDownloadedSubmissions(Assignment).
		*/
	@Transactional
	@Test def downloadedSubmissions(): Unit = withFakeTime(dateTime(2001, 6)) { new Fixture {
		val assignment = {
			val a = newDeepAssignment()
			a.id = "12345"
			val s1 = new Submission
			s1.id = "submissionId1"
			s1.submittedDate = new DateTime().minusHours(5)
			a.submissions add s1
			a
		}

		val command = new NullCommand {
			override lazy val eventName = "DownloadAllSubmissions"

			override def describe(d: Description) = d
				.assignment(assignment)
				.submissions(assignment.submissions.asScala)
				.studentIds(assignment.submissions.asScala.map(_.universityId))
				.properties(
					"submissionCount" -> Option(assignment.submissions).map(_.size).getOrElse(0))
		}

		val auditEvent = {
			val event = Event.fromDescribable(command)
			service.save(event, "before")
			service.getByEventId(event.id)
		}.head

		queryService.adminDownloadedSubmissions(assignment).futureValue should be ('empty)

		// Index the audit event
		client.execute { index into indexName / indexType source auditEvent id auditEvent.id }
		blockUntilCount(1, indexName, indexType)

		queryService.adminDownloadedSubmissions(assignment).futureValue should be (assignment.submissions.asScala)
	}}

	@Transactional
	@Test def individuallyDownloadedSubmissions(): Unit = withFakeTime(dateTime(2001, 6)) { new Fixture {
		val assignment = {
			val a = newDeepAssignment()
			a.id = "54321"
			a.createdDate = new DateTime().minusHours(10)
			val s1 = new Submission
			s1.id = "321"
			s1.assignment = a
			s1.submittedDate = new DateTime().minusHours(5)
			a.submissions add s1
			a
		}

		val command = new NullCommand {
			override lazy val eventName = "AdminGetSingleSubmission"

			override def describe(d: Description) = {
				def submission = assignment.submissions.asScala.head

				d.submission(submission).properties(
					"studentId" -> submission.universityId,
					"attachmentCount" -> submission.allAttachments.size)
			}
		}

		val auditEvent = {
			val event = Event.fromDescribable(command)
			service.save(event, "before")
			service.getByEventId(event.id)
		}.head

		queryService.adminDownloadedSubmissions(assignment).futureValue should be ('empty)

		// Index the audit event
		client.execute { index into indexName / indexType source auditEvent id auditEvent.id }
		blockUntilCount(1, indexName, indexType)

		queryService.adminDownloadedSubmissions(assignment).futureValue should be (assignment.submissions.asScala)
	}}

	@Transactional
	@Test def noteworthySubmissions(): Unit = withFakeTime(dateTime(2001, 6)) { new Fixture {
		val stopwatch = new StopWatch

		val dept = new Department
		dept.code = "zx"

		val assignment = new Assignment
		assignment.id = "4a0ce216-adda-b0b0-c0c0-000000000000"
		// Merry Christmas
		assignment.closeDate = new DateTime(2009,12,25,0,0,0)

		val module = new Module
		module.id = "367a9abd-adda-c0c0-b0b0-000000000000"
		module.assignments = List(assignment).asJava
		module.adminDepartment = dept

		val beforeJsonData = json.writeValueAsString(Map(
			"assignment" -> assignment.id,
			"module" -> module.id,
			"department" -> dept.code
		))

		val afterJsonData = json.writeValueAsString(Map(
			"assignment" -> assignment.id,
			"module" -> module.id,
			"department" -> dept.code,
			"submission" -> "94624c3b-adda-0dd0-b0b0-REPLACE-THIS"
		))

		val afterLateJsonData = json.writeValueAsString(Map(
			"assignment" -> assignment.id,
			"module" -> module.id,
			"department" -> dept.code,
			"submission" -> "94624c3b-adda-0dd0-b0b0-REPLACE-THIS",
			"submissionIsNoteworthy" -> true
		))

		stopwatch.start("creating items")

		val submitBefore = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="ontime"+i, eventType="SubmitAssignment", eventStage="before", userId="bob",
				eventDate=new DateTime(2009,12,1,0,0,0).plusSeconds(i),
				data=beforeJsonData
			)

		val submitAfter = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="ontime"+i, eventType="SubmitAssignment", eventStage="after", userId="bob",
				eventDate=new DateTime(2009,12,1,0,0,0).plusMinutes(5).plusSeconds(i),
				data=afterLateJsonData.replace("REPLACE-THIS", "%012d".format(i))
			)

		val submitBeforeLate = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="late"+i, eventType="SubmitAssignment", eventStage="before", userId="bob",
				eventDate=new DateTime(2010,1,1,0,0,0).plusSeconds(i),
				data=beforeJsonData
			)

		val submitAfterLate = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="late"+i, eventType="SubmitAssignment", eventStage="after", userId="bob",
				eventDate=new DateTime(2010,1,1,0,0,0).plusMinutes(5).plusSeconds(i),
				data=afterJsonData.replace("REPLACE-THIS", "%012d".format(i))
			)

		stopwatch.stop()

		val events = submitBefore ++ submitAfter ++ submitBeforeLate ++ submitAfterLate
		events.foreach { event =>
			event.parsedData = service.parseData(event.data)
			service.save(event)
		}

		// 140 total distinct events
		service.listNewerThan(new DateTime(2009,12,1,0,0,0), 500).size should be (140)

		// 70 new ones, since Christmas
		service.listNewerThan(new DateTime(2009,12,25,0,0,0), 500).size should be (70)

		stopwatch.start("indexing")

		// Index the audit event
		service.listNewerThan(new DateTime(2009,12,1,0,0,0), 500).foreach { auditEvent =>
			client.execute { index into indexName / indexType source auditEvent id auditEvent.id }
		}

		blockUntilExactCount(140, indexName, indexType)

		stopwatch.stop()

		val paged0 = queryService.submissionsForModules(Seq(module), None, 100).futureValue
		paged0.items.length should be (100)
		paged0.totalHits should be (140)

		val paged1 = queryService.submissionsForModules(Seq(module), paged0.lastUpdatedDate, 100).futureValue
		// asked to batch in 100s, but only 40 left
		paged1.items.length should be (40)
		paged1.totalHits should be (40) // This excludes anything before lastUpdatedDate, which may be confusing.

		// check pager for noteworthy submissions
		val paged2 = queryService.noteworthySubmissionsForModules(Seq(module), None, 100).futureValue
		paged2.items.length should be (70)
		paged2.totalHits should be (70)
	}}

}
