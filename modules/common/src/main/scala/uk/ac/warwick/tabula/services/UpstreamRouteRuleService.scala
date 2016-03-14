package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{UpstreamModuleList, Route, UpstreamRouteRule}
import uk.ac.warwick.tabula.data.{AutowiringUpstreamRouteRuleDaoComponent, UpstreamRouteRuleDaoComponent}
import collection.JavaConverters._

trait UpstreamRouteRuleService {

	def saveOrUpdate(list: UpstreamRouteRule): Unit
	def removeAll(): Unit
	def findNormalLoad(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Option[BigDecimal]

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

}

@Service("upstreamRouteRuleService")
class UpstreamRouteRuleServiceImpl
	extends AbstractUpstreamRouteRuleService
		with AutowiringUpstreamRouteRuleDaoComponent

trait UpstreamRouteRuleServiceComponent {
	def upstreamRouteRuleService: UpstreamRouteRuleService
}

trait AutowiringUpstreamRouteRuleServiceComponent extends UpstreamRouteRuleServiceComponent {
	var upstreamRouteRuleService = Wire[UpstreamRouteRuleService]
}
