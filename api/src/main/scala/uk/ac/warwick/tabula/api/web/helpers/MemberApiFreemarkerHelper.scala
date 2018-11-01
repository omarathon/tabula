package uk.ac.warwick.tabula.api.web.helpers

import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap

import freemarker.template.Template
import uk.ac.warwick.tabula.JavaImports.JConcurrentMap
import uk.ac.warwick.tabula.ScalaConcurrentMapHelpers
import uk.ac.warwick.tabula.api.web.helpers.MemberApiFreemarkerHelper.templateCache
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

trait MemberApiFreemarkerHelper {
	self: ScalaFreemarkerConfigurationComponent =>

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

}

object MemberApiFreemarkerHelper {
	val templateCache: ConcurrentHashMap[String, Template] with ScalaConcurrentMapHelpers[String, Template] = JConcurrentMap[String, Template]()

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

	def disabilityFundingStatusToJson(status: DisabilityFundingStatus): Map[String, Any] = Map(
		"code" -> status.code,
		"description" -> status.description
	)
}