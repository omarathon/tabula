package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import scala.collection.JavaConverters._
import uk.ac.warwick.util.web.UriBuilder
import org.apache.http.client.utils.URLEncodedUtils
import java.net.URI
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.convert.{ModuleCodeConverter, SitsStatusCodeConverter, ModeOfAttendanceCodeConverter, RouteCodeConverter}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, CourseAndRouteServiceComponent}
import uk.ac.warwick.tabula.data.{SitsStatusDaoComponent, ModeOfAttendanceDaoComponent}

trait FiltersStudentsBase {

	def courseTypes: JList[CourseType]
	def routes: JList[Route]
	def modesOfAttendance: JList[ModeOfAttendance]
	def yearsOfStudy: JList[JInteger]
	def sprStatuses: JList[SitsStatus]
	def modules: JList[Module]
	def defaultOrder: Seq[Order]
	def sortOrder: JList[Order]
	var otherCriteria: JList[String] = JArrayList()

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
		otherCriteria.asScala.foreach(p => result.addQueryParameter("otherCriteria", p.toString))
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
			"modules" -> modules.asScala.map{_.code}.mkString(","),
			"otherCriteria" -> otherCriteria.asScala.mkString(",")
		)
	}

}

trait DeserializesFilter extends Logging with FiltersStudentsBase with CourseAndRouteServiceComponent with ModeOfAttendanceDaoComponent
	with SitsStatusDaoComponent with ModuleAndDepartmentServiceComponent {

	def deserializeFilter(filterString: String) = {
		val params: Map[String, Seq[String]] = URLEncodedUtils.parse(new URI(null, null, null, filterString, null), "UTF-8").asScala.groupBy(_.getName).map{
			case (name, nameValuePairs) => name -> nameValuePairs.map(_.getValue)
		}
		courseTypes.clear()
		params.get("courseTypes").map{_.foreach{ item =>
			try {
				courseTypes.add(CourseType.fromCourseCode(item))
			} catch {
				case e: IllegalArgumentException =>
					logger.warn(s"Could not deserialize filter with courseType $item")
			}}
		}
		routes.clear()
		params.get("routes").map{_.foreach{ item =>
			val routeCodeConverter = new RouteCodeConverter
			routeCodeConverter.service = courseAndRouteService
			routeCodeConverter.convertRight(item) match {
				case route: Route => routes.add(route)
				case _ => logger.warn(s"Could not deserialize filter with route $item")
			}
		}}
		modesOfAttendance.clear()
		params.get("modesOfAttendance").map{_.foreach{ item =>
			val modeOfAttendanceCodeConverter = new ModeOfAttendanceCodeConverter
			modeOfAttendanceCodeConverter.dao = modeOfAttendanceDao
			modeOfAttendanceCodeConverter.convertRight(item) match {
				case moa: ModeOfAttendance => modesOfAttendance.add(moa)
				case _ => logger.warn(s"Could not deserialize filter with modeOfAttendance $item")
			}
		}}
		yearsOfStudy.clear()
		params.get("yearsOfStudy").map{_.foreach{ item =>
			try {
				yearsOfStudy.add(item.toInt)
			} catch {
				case e: NumberFormatException =>
					logger.warn(s"Could not deserialize filter with yearOfStudy $item")
			}}
		}
		sprStatuses.clear()
		params.get("sprStatuses").map{_.foreach{ item =>
			val sitsStatusCodeConverter = new SitsStatusCodeConverter
			sitsStatusCodeConverter.dao = sitsStatusDao
			sitsStatusCodeConverter.convertRight(item) match {
				case sprStatus: SitsStatus => sprStatuses.add(sprStatus)
				case _ => logger.warn(s"Could not deserialize filter with sprStatus $item")
			}
		}}
		modules.clear()
		params.get("modules").map{_.foreach{ item =>
			val moduleCodeConverter = new ModuleCodeConverter
			moduleCodeConverter.service = moduleAndDepartmentService
			moduleCodeConverter.convertRight(item) match {
				case module: Module => modules.add(module)
				case _ => logger.warn(s"Could not deserialize filter with module $item")
			}
		}}
		otherCriteria.clear()
		params.get("otherCriteria").map{_.foreach{ item => otherCriteria.add(item) }}
	}

}
