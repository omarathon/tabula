package uk.ac.warwick.tabula.profiles.web

import uk.ac.warwick.tabula.data.model._
import java.net.URLEncoder

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 * 
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	private def encoded(string: String) = URLEncoder.encode(string, "UTF-8")
	def home = "/"

	object profile {
		def view(member: Member) = "/view/%s" format (encoded(member.universityId))
		def view(member: Member, meeting: MeetingRecord) = "/view/%s?meeting=%s" format (encoded(member.universityId), encoded(meeting.id))
		def photo(member: Member) = "/view/photo/%s.jpg" format (encoded(member.universityId))
	}
	
	object admin {
		def departmentPermissions(department: Department) = "/admin/department/%s/permissions" format (encoded(department.code))
	}
}
