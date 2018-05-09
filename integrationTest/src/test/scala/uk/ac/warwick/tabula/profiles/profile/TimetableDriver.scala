package uk.ac.warwick.tabula.profiles.profile

import org.apache.http.HttpStatus
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.util.EntityUtils
import uk.ac.warwick.tabula.helpers.ApacheHttpClientUtils
import uk.ac.warwick.tabula.web.FixturesDriver
import uk.ac.warwick.tabula.{AcademicYear, FunctionalTestProperties, LoginDetails}

import scala.util.parsing.json.JSON
import scala.xml.Elem

trait TimetableDriver extends FixturesDriver {

	def setTimetableFor(userId:String, year:AcademicYear, content:Elem) {
		val uri = FunctionalTestProperties.SiteRoot + "/stubTimetable/student"

		val req =
			RequestBuilder.post(uri)
				.addParameter("studentId", userId)
				.addParameter("year", year.toString.replace("/", ""))
				.addParameter("content", content.toString)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def requestWholeYearsTimetableFeedFor(user:LoginDetails, asUser:Option[LoginDetails]=None ):Seq[Map[String,Any]]={
		val requestor = asUser.getOrElse(user)
		// request all the events for the current year
		val startOfYear = AcademicYear.now().firstDay.toDateTimeAtStartOfDay
		val start = startOfYear.getMillis
		val end = startOfYear.plusYears(1).getMillis
		val req =
			RequestBuilder.get(s"${FunctionalTestProperties.SiteRoot}/api/v1/member/${user.warwickId}/timetable/calendar")
				.addParameter("from", start.toString)
				.addParameter("to", end.toString)
				.addParameter("whoFor", user.warwickId)
				.addParameter("forceBasic", "true")
  			.setHeader(ApacheHttpClientUtils.basicAuthHeader(new UsernamePasswordCredentials(requestor.usercode, requestor.password)))

		val rawJSON = httpClient.execute(req.build(), new BasicResponseHandler)
		JSON.parseFull(rawJSON) match {
			case Some(json: Map[String, Any] @unchecked) => json.get("events") match {
				case Some(events: Seq[Map[String, Any]] @unchecked) => events
				case _ => throw new RuntimeException(s"Couldn't parse JSON into sequence\n $rawJSON")
			}
			case _ => throw new RuntimeException(s"Couldn't parse JSON into sequence\n $rawJSON")
		}
	}
}
