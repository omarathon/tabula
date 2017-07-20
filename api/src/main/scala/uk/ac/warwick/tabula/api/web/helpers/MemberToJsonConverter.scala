package uk.ac.warwick.tabula.api.web.helpers

import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap

import freemarker.template.Template
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ScalaConcurrentMapHelpers
import uk.ac.warwick.tabula.api.web.helpers.MemberToJsonConverter._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

import scala.collection.JavaConverters._

/**
	* We use Freemarker to ensure that we use the same logic as in ScalaBeansWrapper, i.e. that we don't expose
	* any properties that the user doesn't have permissions to see.
	*/
trait MemberToJsonConverter {
	self: ScalaFreemarkerConfigurationComponent =>

	def jsonMemberObject(member: Member, deep: Boolean = true): Map[String, Any] = {
		def eval(el: String, model: Any): Option[String] = {
			val template = templateCache.getOrElseUpdate(el, new Template(s"memberJsonApi://$el", el, freemarkerConfiguration))

			val writer = new StringWriter
			template.process(model, writer)
			writer.toString.maybeText
		}

		def str(name: String, model: Any, propertyName: String): Option[(String, String)] =
			eval(s"$${($propertyName)!}", model).map { name -> _ }

		def int(name: String, model: Any, propertyName: String): Option[(String, Int)] =
			eval(s"$${($propertyName?c)!}", model).map { name -> _.toInt }

		def double(name: String, model: Any, propertyName: String): Option[(String, Double)] =
			eval(s"$${($propertyName?c)!}", model).map { name -> _.toDouble }

		def date(name: String, model: Any, propertyName: String): Option[(String, String)] =
			eval(s"$${($propertyName.toString())!}", model).map { name -> _ }

		def boolean(name: String, model: Any, propertyName: String): Option[(String, Boolean)] =
			eval(s"$${($propertyName?string('true', 'false'))!}", model) match {
				case Some("true") => Some(name -> true)
				case Some("false") => Some(name -> false)
				case _ => None
			}

		def canViewProperty(model: Any, propertyName: String): Boolean =
			eval(s"$${$propertyName???string('true', 'false')}", model).contains("true")

		def departmentToJson(department: Department): Map[String, Any] =
			Map("code" -> department.code, "name" -> department.name)

		def moduleToJson(module: Module): Map[String, Any] =
			Map("code" -> module.code, "name" -> module.name, "adminDepartment" -> departmentToJson(module.adminDepartment))

		def courseToJson(course: Course): Map[String, Any] =
			Map("code" -> course.code, "name" -> course.name, "type" -> CourseType.fromCourseCode(course.code).code)

		def routeToJson(route: Route): Map[String, Any] =
			Map("code" -> route.code, "name" -> route.name, "adminDepartment" -> departmentToJson(route.adminDepartment))

		def awardToJson(award: Award): Map[String, Any] =
			Map("code" -> award.code, "name" -> award.name)

		def sitsStatusToJson(status: SitsStatus): Map[String, Any] =
			Map("code" -> status.code, "name" -> status.fullName)

		def modeOfAttendanceToJson(moa: ModeOfAttendance): Map[String, Any] =
			Map("code" -> moa.code, "name" -> moa.fullName)

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

		def addressToJson(address: Address): Map[String, Any] =
			Seq(
				address.line1.maybeText.map { "line1" -> _ },
				address.line2.maybeText.map { "line2" -> _ },
				address.line3.maybeText.map { "line3" -> _ },
				address.line4.maybeText.map { "line4" -> _ },
				address.line5.maybeText.map { "line5" -> _ },
				address.postcode.maybeText.map { "postcode" -> _ },
				address.telephone.maybeText.map { "telephone" -> _ }
			).flatten.toMap

		def nextOfKinToJson(nextOfKin: NextOfKin): Map[String, Any] =
			Seq(
				nextOfKin.firstName.maybeText.map { "firstName" -> _ },
				nextOfKin.lastName.maybeText.map { "lastName" -> _ },
				nextOfKin.relationship.maybeText.map { "relationship" -> _ }
			).flatten.toMap

		def disabilityToJson(disability: Disability): Map[String, Any] = Map(
			"code" -> disability.code,
			"sitsDefinition" -> disability.sitsDefinition,
			"definition" -> disability.definition
		)

		def studentCourseYearDetailsToJson(scyd: StudentCourseYearDetails): Map[String, Any] = Seq(
			str("sceSequenceNumber", scyd, "sceSequenceNumber"),
			int("yearOfStudy", scyd, "yearOfStudy"),
			boolean("casUsed", scyd, "casUsed"),
			boolean("tier4Visa", scyd, "tier4Visa"),
			str("academicYear", scyd, "academicYear.toString"),
			if (canViewProperty(scyd, "route"))
				Some("route", routeToJson(scyd.route))
			else None,
			if (canViewProperty(scyd, "enrolmentStatus"))
				Some("enrolmentStatus", sitsStatusToJson(scyd.enrolmentStatus))
			else None,
			if (canViewProperty(scyd, "enrolmentDepartment"))
				Some("enrolmentDepartment", departmentToJson(scyd.enrolmentDepartment))
			else None,
			if (canViewProperty(scyd, "modeOfAttendance"))
				Some("modeOfAttendance", modeOfAttendanceToJson(scyd.modeOfAttendance))
			else None
		).flatten.toMap

		def studentRelationshipToJson(rel: StudentRelationship): Map[String, Any] = Seq(
			date("startDate", rel, "startDate"),
			date("endDate", rel, "endDate"),
			double("percentage", rel, "percentage"),
			rel.agentMember.map { agent => "agent" -> jsonMemberObject(agent, deep = false) }
		).flatten.toMap

		def moduleRegistrationToJson(reg: ModuleRegistration): Map[String, Any] = Seq(
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

		def studentCourseDetailsToJson(scd: StudentCourseDetails): Map[String, Any] = Seq(
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
				Some("studentCourseYearDetails" -> scd.freshStudentCourseYearDetails.map(studentCourseYearDetailsToJson))
			else None,
			if (canViewProperty(scd, "moduleRegistrations"))
				Some("moduleRegistrations", scd.moduleRegistrations.map(moduleRegistrationToJson))
			else None
		).flatten.toMap

		val studentProperties = member match {
			case student: StudentMember if deep => Seq(
				if (canViewProperty(student, "homeAddress"))
					Some("homeAddress" -> addressToJson(student.homeAddress))
				else None,
				if (canViewProperty(student, "termtimeAddress"))
					Some("termtimeAddress" -> addressToJson(student.termtimeAddress))
				else None,
				if (canViewProperty(student, "nextOfKins"))
					Some("nextOfKins" -> student.nextOfKins.asScala.map(nextOfKinToJson))
				else None,
				if (canViewProperty(student, "disability"))
					student.disability.filter(_.reportable).map { "disability" -> disabilityToJson(_) }
				else None,
				if (canViewProperty(student, "freshStudentCourseDetails"))
					Some("studentCourseDetails" -> student.freshStudentCourseDetails.map(studentCourseDetailsToJson))
				else None,
				boolean("tier4VisaRequirement", student, "tier4VisaRequirement")
			).flatten.toMap
			case _ => Map()
		}

		val staffProperties = member match {
			case staff @ (_: StaffMember | _: EmeritusMember) if deep => Map() // no additional properties
			case _ => Map()
		}

		memberProperties ++ studentProperties ++ staffProperties
	}

}

object MemberToJsonConverter {
	val templateCache: ConcurrentHashMap[String, Template] with ScalaConcurrentMapHelpers[String, Template] = JConcurrentMap[String, Template]()
}
