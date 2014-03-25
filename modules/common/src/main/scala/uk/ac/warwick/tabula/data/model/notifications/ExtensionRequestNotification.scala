package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.{StudentMember, FreemarkerModel}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

abstract class ExtensionRequestNotification extends ExtensionNotification {

	@transient
	var relationshipService = Wire.auto[RelationshipService]
	@transient
	var profileService = Wire.auto[ProfileService]

	def template: String

	def url = Routes.admin.assignment.extension.expandrow(assignment, student.getWarwickId)
	def urlTitle = "review this extension request"

	def studentMember = profileService.getMemberByUniversityId(student.getWarwickId)
	def studentRelationships = relationshipService.allStudentRelationshipTypes

	def actionRequired = true

	def profileInfo = studentMember.collect { case student: StudentMember => student }.flatMap(_.mostSignificantCourseDetails).map(scd => {
		val relationships = studentRelationships.map(x => (
			x.description,
			relationshipService.findCurrentRelationships(x, scd)
		)).filter{ case (relationshipType,relations) => relations.length != 0 }.toMap

		Map(
			"relationships" -> relationships,
			"scdCourse" -> scd.course,
			"scdRoute" -> scd.route,
			"scdAward" -> scd.award
		)
	}).getOrElse(Map())

	def content = FreemarkerModel(template, Map(
		"requestedExpiryDate" -> dateTimeFormatter.print(extension.requestedExpiryDate),
		"reasonForRequest" -> extension.reason,
		"attachments" -> extension.attachments,
		"assignment" -> assignment,
		"student" -> student,
		"moduleManagers" -> assignment.module.managers.users
	) ++ profileInfo)

	def recipients = assignment.module.department.extensionManagers.users
}

@Entity
@DiscriminatorValue("ExtensionRequestCreated")
class ExtensionRequestCreatedNotification extends ExtensionRequestNotification {
	priority = Warning
	def verb = "create"
	def template = "/WEB-INF/freemarker/emails/new_extension_request.ftl"
	def title = titlePrefix + "New extension request made"
}

@Entity
@DiscriminatorValue("ExtensionRequestModified")
class ExtensionRequestModifiedNotification extends ExtensionRequestNotification {
	priority = Warning
	def verb = "modify"
	def template = "/WEB-INF/freemarker/emails/modified_extension_request.ftl"
	def title = titlePrefix + "Extension request modified"
}