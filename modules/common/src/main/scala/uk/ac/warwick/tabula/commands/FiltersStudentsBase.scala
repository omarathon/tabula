package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.{Module, SitsStatus, ModeOfAttendance, Route, CourseType, Department}
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import scala.collection.JavaConverters._
import uk.ac.warwick.util.web.UriBuilder

trait FiltersStudentsBase {

	def courseTypes: JList[CourseType]
	def routes: JList[Route]
	def modesOfAttendance: JList[ModeOfAttendance]
	def yearsOfStudy: JList[JInteger]
	def sprStatuses: JList[SitsStatus]
	def modules: JList[Module]
	def defaultOrder: Seq[Order]
	def sortOrder: JList[Order]

	protected def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
		(department.modules.asScala ++ department.children.asScala.flatMap { modulesForDepartmentAndSubDepartments }).sorted

	protected def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
		(department.routes.asScala ++ department.children.asScala.flatMap { routesForDepartmentAndSubDepartments }).sorted

	def serializeFilter = {
		val result = new UriBuilder()
		courseTypes.asScala.foreach(p => result.addQueryParameter("courseTypes", p.code))
		routes.asScala.foreach(p => result.addQueryParameter("routes", p.code))
		modesOfAttendance.asScala.foreach(p => result.addQueryParameter("modesOfAttendance", p.code))
		yearsOfStudy.asScala.foreach(p => result.addQueryParameter("yearsOfStudy", p.toString))
		sprStatuses.asScala.foreach(p => result.addQueryParameter("sprStatuses", p.code))
		modules.asScala.foreach(p => result.addQueryParameter("modules", p.code))
		if (result.getQuery == null)
			""
		else
			result.getQuery
	}

	def filterMap = {
		Map(
			"courseTypes" -> courseTypes.asScala.map{_.code}.mkString(","),
			"routes" -> routes.asScala.map{_.code}.mkString(","),
			"modesOfAttendance" -> modesOfAttendance.asScala.map{_.code}.mkString(","),
			"yearsOfStudy" -> yearsOfStudy.asScala.mkString(","),
			"sprStatuses" -> sprStatuses.asScala.map{_.code}.mkString(","),
			"modules" -> modules.asScala.map{_.code}.mkString(",")
		)
	}

}
