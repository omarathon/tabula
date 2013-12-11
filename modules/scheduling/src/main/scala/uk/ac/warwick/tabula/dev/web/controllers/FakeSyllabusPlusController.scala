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
import uk.ac.warwick.tabula.helpers.Logging
import scala.util.{Success, Try}
import uk.ac.warwick.tabula.AcademicYear

/**
 * Proxy student requests to Syllabus+, cacheing responses.
 *
 * Allows for the override of individual student responses, for testing purposes.
 *
 * TODO add in support for all the other kinds of timetable requests
 */
@Controller
class FakeSyllabusPlusController extends Logging {

	val userLookup:UserLookupService = Wire[UserLookupService]
	val studentTimetables: mutable.Map[StudentYearKey, String] = mutable.Map.empty
	val baseUri = "https://timetablingmanagement.warwick.ac.uk/xml"
	def studentUri(year:String) = baseUri + year + "/?StudentXML"


	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	@RequestMapping(Array("/stubTimetable/student{year}/"))
	def getStudent(@RequestParam("p0") studentId: String, @PathVariable("year") year:String, output: Writer) {
		val xml = studentTimetables.getOrElseUpdate(StudentYearKey(studentId, year) , {
			val req = url(studentUri(year)) <<? Map("p0" -> studentId)
			import scala.language.postfixOps
			val xml = Try(http.when(_==200)(req as_str)) match {
				case Success(s)=>s
				// If we get an error back, just return then XML for an empty list immediately,
				// otherwise the XML handler in ScientiaHttpTimetableFetchingService
				// will wait for the request keep-alive to time out (60s) before finally giving up.
				// n.b. if we made this controller return a non-200 status code then we probably wouldn't have
			  // to do this.
				case _	=>"<?xml version=\"1.0\" encoding=\"utf-8\"?><Data><Activities></Activities></Data>"
			}
			xml
		})
		output.write(xml)
	}

	// note that the "year" variable should be in the same format the Syallabus+ uses
	// i.e. 1213 for academic year 2012-2013
	@RequestMapping(method = Array(RequestMethod.POST), value = Array("/stubTimetable/student"))
	def saveStudent(@RequestParam studentId: String, @RequestParam year:String, @RequestParam content: String) {
		if (!studentId.matches("^[0-9]+")){
			// it's probably a usercode, since functional tests don't have access  to warwickIds for their users
			val user = userLookup.getUserByUserId(studentId)
			studentTimetables.put(StudentYearKey(user.getWarwickId, year), content)
		}else{
			studentTimetables.put(StudentYearKey(studentId, year), content)
		}
	}

}
case class StudentYearKey(studentId:String, year:String)
