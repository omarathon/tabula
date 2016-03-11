package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

trait UpstreamRouteRuleDaoComponent {
	val upstreamRouteRuleDao: UpstreamRouteRuleDao
}

trait AutowiringUpstreamRouteRuleDaoComponent extends UpstreamRouteRuleDaoComponent {
	val upstreamRouteRuleDao = Wire[UpstreamRouteRuleDao]
}

trait UpstreamRouteRuleDao {
	def saveOrUpdate(list: UpstreamRouteRule): Unit
	def list(route: Route, academicYearOption: Option[AcademicYear], yearOfStudy: Int): Seq[UpstreamRouteRule]
	def removeAll(): Unit
}

@Repository
class UpstreamRouteRuleDaoImpl extends UpstreamRouteRuleDao with Daoisms {

	def saveOrUpdate(list: UpstreamRouteRule): Unit =
		session.saveOrUpdate(list)

	def list(route: Route, academicYearOption: Option[AcademicYear], yearOfStudy: Int): Seq[UpstreamRouteRule] = {
		session.newCriteria[UpstreamRouteRule]
			.add(is("route", route))
			.add(is("academicYear", academicYearOption.orNull))
			.add(is("yearOfStudy", yearOfStudy))
			.seq
	}

	def removeAll(): Unit = {
		session.createSQLQuery("delete from UpstreamRouteRuleEntry").executeUpdate()
		session.createSQLQuery("delete from UpstreamRouteRule").executeUpdate()
	}

}
