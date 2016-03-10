package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Route, UpstreamRouteRule}
import uk.ac.warwick.tabula.data.{AutowiringUpstreamRouteRuleDaoComponent, UpstreamRouteRuleDaoComponent}

trait UpstreamRouteRuleService {

	def saveOrUpdate(list: UpstreamRouteRule): Unit
	def list(route: Route, academicYearOption: Option[AcademicYear], yearOfStudy: Int): Seq[UpstreamRouteRule]
	def removeAll(): Unit

}

abstract class AbstractUpstreamRouteRuleService extends UpstreamRouteRuleService {

	self: UpstreamRouteRuleDaoComponent =>

	def saveOrUpdate(list: UpstreamRouteRule): Unit =
		upstreamRouteRuleDao.saveOrUpdate(list)

	def list(route: Route, academicYearOption: Option[AcademicYear], yearOfStudy: Int): Seq[UpstreamRouteRule] =
		upstreamRouteRuleDao.list(route, academicYearOption, yearOfStudy)

	def removeAll(): Unit =
		upstreamRouteRuleDao.removeAll()

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
