package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.api.web.helpers.MemberApiFreemarkerHelper._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

import scala.collection.JavaConverters._

/**
	* We use Freemarker to ensure that we use the same logic as in ScalaBeansWrapper, i.e. that we don't expose
	* any properties that the user doesn't have permissions to see.
	*/
trait MemberToJsonConverter
	extends StudentCourseDetailsToJsonConverter
		with StudentCourseYearDetailsToJsonConverter
		with MemberApiFreemarkerHelper {
	self: ScalaFreemarkerConfigurationComponent =>

	def jsonMemberObject(member: Member, deep: Boolean = true): Map[String, Any] = {
		val memberProperties = Seq(
			str("universityId", member, "universityId"),
			str("userId", member, "userId"),
			str("firstName", member, "firstName"),
			str("lastName", member, "lastName"),
			str("email", member, "email"),
			str("userType", member, "userType.description"),
			str("fullName", member, "fullName"),
			str("officialName", member, "officialName"),
			str("homeEmail", member, "homeEmail"),
			str("fullFirstName", member, "fullFirstName"),
			str("title", member, "title"),
			str("gender", member, "gender.description"),
			str("inUseFlag", member, "inUseFlag"),
			str("jobTitle", member, "jobTitle"),
			str("phoneNumber", member, "phoneNumber"),
			str("nationality", member, "nationality"),
			str("secondNationality", member, "secondNationality"),
			str("mobileNumber", member, "mobileNumber"),
			str("groupName", member, "groupName"),
			if (canViewProperty(member, "affiliatedDepartments"))
				Some("affiliatedDepartments" -> member.affiliatedDepartments.map(departmentToJson))
			else None,
			if (canViewProperty(member, "touchedDepartments"))
				Some("touchedDepartments" -> member.touchedDepartments.map(departmentToJson))
			else None,
			if (canViewProperty(member, "homeDepartment"))
				Some("homeDepartment", departmentToJson(member.homeDepartment))
			else None,
			date("inactivationDate", member, "inactivationDate"),
			date("dateOfBirth", member, "dateOfBirth")
		).flatten.toMap

		val applicantAndStudentProperties = member match {
			case m : ApplicantProperties if deep => Seq(
				if (canViewProperty(m, "currentAddress"))
					Some("currentAddress" -> addressToJson(m.currentAddress))
				else None,
				if (canViewProperty(m, "disability"))
					m.disability.filter(_.reportable).map { "disability" -> disabilityToJson(_) }
				else None,
			).flatten.toMap
			case _ => Map()
		}

		val studentProperties = member match {
			case student: StudentMember if deep => Seq(
				if (canViewProperty(student, "termtimeAddress"))
					Some("termtimeAddress" -> addressToJson(student.termtimeAddress))
				else None,
				if (canViewProperty(student, "nextOfKins"))
					Some("nextOfKins" -> student.nextOfKins.asScala.map(nextOfKinToJson))
				else None,
				if (canViewProperty(student, "freshStudentCourseDetails"))
					Some("studentCourseDetails" -> student.freshStudentCourseDetails.map(jsonStudentCourseDetailsObject))
				else None,
				boolean("tier4VisaRequirement", student, "tier4VisaRequirement")
			).flatten.toMap
			case _ => Map()
		}

		val staffProperties = member match {
			case staff @ (_: StaffMember | _: EmeritusMember) if deep => Map() // no additional properties
			case _ => Map()
		}

		memberProperties ++ applicantAndStudentProperties ++ studentProperties ++ staffProperties
	}

}
