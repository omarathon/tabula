package uk.ac.warwick.tabula.coursework.web

import java.net.URLEncoder
import uk.ac.warwick.tabula.data.model.{Module, MarkingWorkflow, Department, Assignment}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/coursework"
	def home = context + "/"

	object assignment {
		def apply(assignment: Assignment) = context + "/module/%s/%s/" format (encoded(assignment.module.code), encoded(assignment.id))
		def receipt(assignment: Assignment) = apply(assignment)
	}

	object admin {
		def department(department: Department) = context + "/admin/department/%s/" format (encoded(department.code))
		def feedbackTemplates (department: Department) = context + "/admin/department/%s/settings/feedback-templates/" format (encoded(department.code))
		def feedbackReports (department: Department) = context + "/admin/department/%s/reports/feedback/" format (encoded(department.code))

		object markingWorkflow {
			def list(department: Department) = admin.department(department) + "/markingworkflows"
			def add(department: Department) = list(department) + "/add"
			def edit(scheme: MarkingWorkflow) = list(scheme.department) + "/edit/" + scheme.id
		}

		object module {
			def apply(module: Module) = department(module.department) + "#module-" + encoded(module.code)
		}

		object assignment {
			object markerFeedback {
				def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/list"
			}

			object onlineFeedback {
				def apply(assignment: Assignment) = assignmentroot(assignment) + "/feedback/online"
			}

			object onlineMarkerFeedback {
				def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/feedback/online"
			}

			object onlineModeration {
				def apply(assignment: Assignment) = assignmentroot(assignment) + "/marker/feedback/online/moderation"
			}

			def create(module: Module) = context + "/admin/module/%s/assignments/new" format (encoded(module.code))

			private def assignmentroot(assignment: Assignment) = context + "/admin/module/%s/assignments/%s" format (encoded(assignment.module.code), assignment.id)

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
				def detail (assignment: Assignment) = assignmentroot(assignment) + "/extensions/detail"
			}
		}
	}

}
