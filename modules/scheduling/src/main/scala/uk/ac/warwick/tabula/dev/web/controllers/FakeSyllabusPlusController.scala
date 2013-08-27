package uk.ac.warwick.tabula.dev.web.controllers

import dispatch.classic.{url, thread, Http}
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import org.springframework.web.bind.annotation.{RequestMethod, RequestParam, PathVariable, RequestMapping}
import java.io.Writer
import org.springframework.stereotype.Controller
import scala.collection.mutable
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire

/**
 * Proxy student requests to Syllabus+, cacheing responses.
 *
 * Allows for the override of individual student responses, for testing purposes.
 *
 * TODO add in support for all the other kinds of timetable requests
 */
@Controller
@RequestMapping(Array("/stubTimetable/*"))
class FakeSyllabusPlusController {

	val userLookup:UserLookupService = Wire[UserLookupService]
	val studentTimetables: mutable.Map[String, String] = mutable.Map.empty
	val baseUri = "https://test-timetablingmanagement.warwick.ac.uk/xml/"
	lazy val studentUri = baseUri + "?StudentXML"


	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	@RequestMapping(Array("/stubTimetable/student"))
	def getStudent(@RequestParam("p0") studentId: String, output: Writer) {

		val xml = studentTimetables.getOrElseUpdate(studentId, {
			val req = url(studentUri) <<? Map("p0" -> studentId)
			import scala.language.postfixOps
			http.x(req as_str)
		})
		output.write(xml)
	}

	@RequestMapping(method = Array(RequestMethod.POST), value = Array("/stubTimetable/student"))
	def saveStudent(@RequestParam studentId: String, @RequestParam content: String) {
		if (!studentId.matches("^[0-9]+")){
			// it's probably a usercode, since functional tests don't have access  to warwickIds for their users
			val user = userLookup.getUserByUserId(studentId)
			studentTimetables.put(user.getWarwickId, content)
		}else{
			studentTimetables.put(studentId, content)
		}
	}

}
