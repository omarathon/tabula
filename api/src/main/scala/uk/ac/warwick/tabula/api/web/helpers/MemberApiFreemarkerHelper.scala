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

	def str(name: String, model: Any, propertyName: String, fieldRestriction: APIFieldRestriction): Option[(String, String)] =
		fieldRestriction.restrict(name) {
			eval(s"$${($propertyName)!}", model).map { name -> _ }
		}

	def int(name: String, model: Any, propertyName: String, fieldRestriction: APIFieldRestriction): Option[(String, Int)] =
		fieldRestriction.restrict(name) {
			eval(s"$${($propertyName?c)!}", model).map { name -> _.toInt }
		}

	def double(name: String, model: Any, propertyName: String, fieldRestriction: APIFieldRestriction): Option[(String, Double)] =
		fieldRestriction.restrict(name) {
			eval(s"$${($propertyName?c)!}", model).map { name -> _.toDouble }
		}

	def date(name: String, model: Any, propertyName: String, fieldRestriction: APIFieldRestriction): Option[(String, String)] =
		fieldRestriction.restrict(name) {
			eval(s"$${($propertyName.toString())!}", model).map { name -> _ }
		}

	def boolean(name: String, model: Any, propertyName: String, fieldRestriction: APIFieldRestriction): Option[(String, Boolean)] =
		fieldRestriction.restrict(name) {
			eval(s"$${($propertyName?string('true', 'false'))!}", model) match {
				case Some("true") => Some(name -> true)
				case Some("false") => Some(name -> false)
				case _ => None
			}
		}

	def canViewProperty(model: Any, propertyName: String): Boolean =
		eval(s"$${$propertyName???string('true', 'false')}", model).contains("true")

}

object MemberApiFreemarkerHelper {
	val templateCache: ConcurrentHashMap[String, Template] with ScalaConcurrentMapHelpers[String, Template] = JConcurrentMap[String, Template]()

	def departmentToJson(department: Department, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> department.code) },
			fieldRestriction.restrict("name") { Some("name" -> department.name) },
		).flatten.toMap

	def moduleToJson(module: Module, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> module.code) },
			fieldRestriction.restrict("name") { Some("name" -> module.name) },
			fieldRestriction.nested("adminDepartment").flatMap { restriction =>
				Some("adminDepartment" -> departmentToJson(module.adminDepartment, restriction))
			},
		).flatten.toMap

	def courseToJson(course: Course, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> course.code) },
			fieldRestriction.restrict("name") { Some("name" -> course.name) },
			fieldRestriction.restrict("type") { Some("type" -> CourseType.fromCourseCode(course.code).code) },
		).flatten.toMap

	def routeToJson(route: Route, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> route.code) },
			fieldRestriction.restrict("name") { Some("name" -> route.name) },
			fieldRestriction.nested("adminDepartment").flatMap { restriction =>
				Some("adminDepartment" -> departmentToJson(route.adminDepartment, restriction))
			},
		).flatten.toMap

	def awardToJson(award: Award, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> award.code) },
			fieldRestriction.restrict("name") { Some("name" -> award.name) },
		).flatten.toMap

	def sitsStatusToJson(status: SitsStatus, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> status.code) },
			fieldRestriction.restrict("name") { Some("name" -> status.fullName) },
		).flatten.toMap

	def modeOfAttendanceToJson(moa: ModeOfAttendance, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> moa.code) },
			fieldRestriction.restrict("name") { Some("name" -> moa.fullName) },
		).flatten.toMap

	def addressToJson(address: Address, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("line1") { address.line1.maybeText.map { "line1" -> _ } },
			fieldRestriction.restrict("line2") { address.line2.maybeText.map { "line2" -> _ } },
			fieldRestriction.restrict("line3") { address.line3.maybeText.map { "line3" -> _ } },
			fieldRestriction.restrict("line4") { address.line4.maybeText.map { "line4" -> _ } },
			fieldRestriction.restrict("line5") { address.line5.maybeText.map { "line5" -> _ } },
			fieldRestriction.restrict("postcode") { address.postcode.maybeText.map { "postcode" -> _ } },
			fieldRestriction.restrict("telephone") { address.telephone.maybeText.map { "telephone" -> _ } }
		).flatten.toMap

	def nextOfKinToJson(nextOfKin: NextOfKin, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("firstName") { nextOfKin.firstName.maybeText.map { "firstName" -> _ } },
			fieldRestriction.restrict("lastName") { nextOfKin.lastName.maybeText.map { "lastName" -> _ } },
			fieldRestriction.restrict("relationship") { nextOfKin.relationship.maybeText.map { "relationship" -> _ } }
		).flatten.toMap

	def disabilityToJson(disability: Disability, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> disability.code) },
			fieldRestriction.restrict("sitsDefinition") { Some("sitsDefinition" -> disability.sitsDefinition) },
			fieldRestriction.restrict("definition") { Some("definition" -> disability.definition) },
		).flatten.toMap

	def disabilityFundingStatusToJson(status: DisabilityFundingStatus, fieldRestriction: APIFieldRestriction): Map[String, Any] =
		Seq(
			fieldRestriction.restrict("code") { Some("code" -> status.code) },
			fieldRestriction.restrict("description") { Some("description" -> status.description) },
		).flatten.toMap
}