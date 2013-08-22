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
	def search = "/search"
		
	object profile {
		def view(member: Member) = "/view/%s" format (encoded(member.universityId))
		def view(member: Member, meeting: MeetingRecord) = "/view/%s?meeting=%s" format (encoded(member.universityId), encoded(meeting.id))
		def photo(member: Member) = "/view/photo/%s.jpg" format (encoded(member.universityId))
	}
	
	def tutees = "/tutees"	
	object tutors {
		def apply(department: Department) = "/department/%s/tutors" format (encoded(department.code))
		def missing(department: Department) = "/department/%s/tutors/missing" format (encoded(department.code))
		def allocate(department: Department) = "/department/%s/tutors/allocate" format (encoded(department.code))
		def template(department: Department) = "/department/%s/tutors/template" format (encoded(department.code))
	}
	
	object supervisor {
		def supervisees = "/supervisees"
	}
	
	object admin {
		def apply(department: Department) = Routes.home // TODO https://repo.elab.warwick.ac.uk/projects/TAB/repos/tabula/pull-requests/145/overview?commentId=1012
		def departmentPermissions(department: Department) = "/admin/department/%s/permissions" format (encoded(department.code))
	}
}
