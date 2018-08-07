package uk.ac.warwick.tabula.data.model.notifications

import java.io.{ByteArrayOutputStream, OutputStreamWriter}

import org.joda.time.DateTimeUtils
import org.junit.After
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, TestBase}
import uk.ac.warwick.tabula.data.model.{Department, MemberUserType, Notification}

class ReportStudentsConfirmNotificationTest extends TestBase {

	@After
	def setTimeBack(): Unit = DateTimeUtils.setCurrentMillisSystem()

	@Test
	def rendersCorrectly(): Unit = withUser("u1574595", "1574595") {


		val acadYear = AcademicYear.parse("17/18")
		val period = "inner peace vacation"
		val dept = Fixtures.department("TSF", "tabula superhero factory")

		val mon1 = new MonitoringPointReport()
		mon1.academicYear = acadYear
		mon1.monitoringPeriod = period
		mon1.studentCourseDetails = Fixtures.studentCourseDetails(
			Fixtures.student(
				"id1",
				"id1",
				dept
			),
			dept
		)

		val mon2 = new MonitoringPointReport()
		mon2.academicYear = acadYear
		mon2.monitoringPeriod = period
		mon2.studentCourseDetails = Fixtures.studentCourseDetails(
			Fixtures.student(
				"id2",
				"id2",
				dept
			),
			dept
		)

		// 20 Dec 2020 00:00:00
		DateTimeUtils.setCurrentMillisFixed(1608422400000L)

		val notification = Notification.init(
			notification = new ReportStudentsConfirmNotification,
			agent = currentUser.realUser,
			items = Seq(
				mon1,
				mon2
			)
		)
		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(notification.templateLocation)
		template.process(notification.content.model, writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be("u1574595 in tabula superhero factory uploaded missed monitoring points from Tabula to SITS on 20 December 2020 at 00:00:00.\n\nAcademic year: 2017/2018\nTerm: inner peace vacation\nStudents count: 2")

	}

}
