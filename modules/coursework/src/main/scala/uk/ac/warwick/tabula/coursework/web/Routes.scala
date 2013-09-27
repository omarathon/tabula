package uk.ac.warwick.tabula.coursework.web

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

	object assignment {
		def apply(assignment: Assignment) = "/module/%s/%s/" format (encoded(assignment.module.code), encoded(assignment.id))
		def receipt(assignment: Assignment) = apply(assignment)
	}

	object admin {
		def department(department: Department) = "/admin/department/%s/" format (encoded(department.code))
		def feedbackTemplates (department: Department) = "/admin/department/%s/settings/feedback-templates/" format (encoded(department.code))
		def feedbackReports (department: Department) = "/admin/department/%s/reports/feedback/" format (encoded(department.code))

		object markingWorkflow {
			def list(department: Department) = admin.department(department) + "/markingworkflows"
			def add(department: Department) = list(department) + "/add"
			def edit(scheme: MarkingWorkflow) = list(scheme.department) + "/edit/" + scheme.id
		}
		
		object module {
			def apply(module: Module) = department(module.department) + "#module-" + encoded(module.code)
			def create() = "/admin/module/add"
		}

		object assignment {
			object markerFeedback {
				def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/list"
			}

			object onlineMarkerFeedback {
				def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/feedback/online"
			}


			def create(module: Module) = "/admin/module/%s/assignments/new" format (encoded(module.code))

			private def assignmentroot(assignment: Assignment) = "/admin/module/%s/assignments/%s" format (encoded(assignment.module.code), assignment.id)

			def edit(assignment: Assignment) = assignmentroot(assignment) + "/edit"

			def delete(assignment: Assignment) = assignmentroot(assignment) + "/delete"

			def submissionsZip(assignment: Assignment) = assignmentroot(assignment) + "/submissions.zip"
			
			object submissionsandfeedback {
				def apply(assignment: Assignment) = assignmentroot(assignment) + "/list"
				def summary(assignment: Assignment) = assignmentroot(assignment) + "/summary"
				def table(assignment: Assignment) = assignmentroot(assignment) + "/table"
			}

			object turnitin {
				def status(assignment: Assignment) = assignmentroot(assignment) + "/turnitin"
			}

			object extension {
				def review(assignment: Assignment, uniId: String) = assignmentroot(assignment) + "/extensions?highlight=" + uniId
			}
		}
	}

	object sysadmin {
		def home = "/sysadmin"

		object events {
			def query = "/sysadmin/audit/search"
		}
	}
}

// Could do something like this to centralise all the @RequestMapping locations?
/*object Mappings {
	object admin {
		final val settings = "/admin/module/{module}/settings"
		object assignment {
			final val create = "/admin/module/{module}/assignments/new"
		}
	}
}*/

