package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Route, UpstreamModuleList, UpstreamRouteRule}
import uk.ac.warwick.tabula.data.{AutowiringUpstreamRouteRuleDaoComponent, UpstreamRouteRuleDaoComponent}

import collection.JavaConverters._
import scala.collection.mutable

class NormalLoadLookup(academicYear: AcademicYear, yearOfStudy: YearOfStudy, upstreamRouteRuleService: UpstreamRouteRuleService) {
	private val cache = mutable.Map[Route, Option[BigDecimal]]()
	def withoutDefault(route: Route): Option[BigDecimal] = cache.get(route) match {
		case Some(option) =>
			option
		case _ =>
			cache.put(route, upstreamRouteRuleService.findNormalLoad(route, academicYear, yearOfStudy))
			cache(route)
	}
	def apply(route: Route): BigDecimal = withoutDefault(route).getOrElse(route.degreeType.normalCATSLoad)
	def routes: Seq[Route] = cache.keys.toSeq
}

trait UpstreamRouteRuleService {

	def saveOrUpdate(list: UpstreamRouteRule): Unit
	def removeAll(): Unit
	def findNormalLoad(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Option[BigDecimal]
	def list(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[UpstreamRouteRule]

}

abstract class AbstractUpstreamRouteRuleService extends UpstreamRouteRuleService {

	self: UpstreamRouteRuleDaoComponent =>

	def saveOrUpdate(list: UpstreamRouteRule): Unit =
		upstreamRouteRuleDao.saveOrUpdate(list)

	def removeAll(): Unit =
		upstreamRouteRuleDao.removeAll()

	def findNormalLoad(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Option[BigDecimal] = {
		val rules = upstreamRouteRuleDao.list(route, academicYear, yearOfStudy)
		val ruleEntries = rules.flatMap(_.entries.asScala.toSeq)
		val relevantRules = ruleEntries.filter(entry => entry.minCats.nonEmpty && entry.list.code == UpstreamModuleList.AllModulesListCode)
		if (relevantRules.isEmpty) {
			None
		} else {
			Option(relevantRules.flatMap(_.minCats).max)
		}
	}

	def list(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[UpstreamRouteRule] =
		upstreamRouteRuleDao.list(route, academicYear, yearOfStudy)

}

@Service("upstreamRouteRuleService")
class UpstreamRouteRuleServiceImpl
	extends AbstractUpstreamRouteRuleService
		with AutowiringUpstreamRouteRuleDaoComponent

trait UpstreamRouteRuleServiceComponent {
	def upstreamRouteRuleService: UpstreamRouteRuleService
}

trait AutowiringUpstreamRouteRuleServiceComponent extends UpstreamRouteRuleServiceComponent {
	var upstreamRouteRuleService: UpstreamRouteRuleService = Wire[UpstreamRouteRuleService]
}
