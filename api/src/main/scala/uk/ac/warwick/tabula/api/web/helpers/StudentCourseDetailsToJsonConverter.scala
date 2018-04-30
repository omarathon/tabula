package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.api.web.helpers.MemberApiFreemarkerHelper._
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, StudentCourseDetails, StudentRelationship}
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

import scala.collection.JavaConverters._

/**
	* We use Freemarker to ensure that we use the same logic as in ScalaBeansWrapper, i.e. that we don't expose
	* any properties that the user doesn't have permissions to see.
	*/
trait StudentCourseDetailsToJsonConverter
	extends StudentCourseYearDetailsToJsonConverter
		with MemberApiFreemarkerHelper {
	self: ScalaFreemarkerConfigurationComponent with MemberToJsonConverter =>

	private def studentRelationshipToJson(rel: StudentRelationship): Map[String, Any] = Seq(
		date("startDate", rel, "startDate"),
		date("endDate", rel, "endDate"),
		double("percentage", rel, "percentage"),
		rel.agentMember.map { agent => "agent" -> jsonMemberObject(agent, deep = false) }
	).flatten.toMap

	private def moduleRegistrationToJson(reg: ModuleRegistration): Map[String, Any] = Seq(
		if (canViewProperty(reg, "module"))
			Some("module", moduleToJson(reg.module))
		else None,
		double("cats", reg, "cats"),
		str("academicYear", reg, "academicYear.toString"),
		str("assessmentGroup", reg, "assessmentGroup"),
		str("occurrence", reg, "occurrence"),
		double("mark", reg, "agreedMark"),
		str("grade", reg, "agreedGrade"),
		str("status", reg, "selectionStatus.description")
	).flatten.toMap

	def jsonStudentCourseDetailsObject(scd: StudentCourseDetails): Map[String, Any] = Seq(
		str("scjCode", scd, "scjCode"),
		str("sprCode", scd, "sprCode"),
		str("levelCode", scd, "levelCode"),
		date("beginDate", scd, "beginDate"),
		date("endDate", scd, "endDate"),
		date("expectedEndDate", scd, "expectedEndDate"),
		str("courseYearLength", scd, "courseYearLength"),
		boolean("mostSignificant", scd, "mostSignificant"),
		str("reasonForTransferCode", scd, "reasonForTransferCode"),
		if (canViewProperty(scd, "course"))
			Some("course", courseToJson(scd.course))
		else None,
		if (canViewProperty(scd, "currentRoute"))
			Some("currentRoute", routeToJson(scd.currentRoute))
		else None,
		if (canViewProperty(scd, "department"))
			Some("department", departmentToJson(scd.department))
		else None,
		if (canViewProperty(scd, "award"))
			Some("award", awardToJson(scd.award))
		else None,
		if (canViewProperty(scd, "statusOnRoute"))
			Some("statusOnRoute", sitsStatusToJson(scd.statusOnRoute))
		else None,
		if (canViewProperty(scd, "statusOnCourse"))
			Some("statusOnCourse", sitsStatusToJson(scd.statusOnCourse))
		else None,
		if (canViewProperty(scd, "relationships"))
			Some("relationships" -> scd.allRelationships.asScala.filter(_.isCurrent).groupBy(_.relationshipType).map { case (relType, relationships) =>
				relType.urlPart -> relationships.toSeq.map(studentRelationshipToJson)
			})
		else None,
		if (canViewProperty(scd, "freshStudentCourseYearDetails"))
			Some("studentCourseYearDetails" -> scd.freshStudentCourseYearDetails.map(jsonStudentCourseYearDetailsObject))
		else None,
		if (canViewProperty(scd, "moduleRegistrations"))
			Some("moduleRegistrations", scd.moduleRegistrations.map(moduleRegistrationToJson))
		else None
	).flatten.toMap

}
