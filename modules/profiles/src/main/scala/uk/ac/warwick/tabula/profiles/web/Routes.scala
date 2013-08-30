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
	
	def students(relationshipType: StudentRelationshipType) = "/%s/students" format (encoded(relationshipType.urlPart))		
		
	object relationships {
		def apply(department: Department, relationshipType: StudentRelationshipType) = "/department/%s/%s/all" format (encoded(department.code), encoded(relationshipType.urlPart))
		def missing(department: Department, relationshipType: StudentRelationshipType) = "/department/%s/%s/missing" format (encoded(department.code), encoded(relationshipType.urlPart))
		def allocate(department: Department, relationshipType: StudentRelationshipType) = "/department/%s/%s/allocate" format (encoded(department.code), encoded(relationshipType.urlPart))
		def template(department: Department, relationshipType: StudentRelationshipType) = "/department/%s/%s/template" format (encoded(department.code), encoded(relationshipType.urlPart))
	}
	
	object admin {
		def apply(department: Department) = Routes.home // TODO https://repo.elab.warwick.ac.uk/projects/TAB/repos/tabula/pull-requests/145/overview?commentId=1012
		def departmentPermissions(department: Department) = "/admin/department/%s/permissions" format (encoded(department.code))
	}
}
