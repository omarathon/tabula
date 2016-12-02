package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.{Route, UpstreamModuleList, UpstreamRouteRule, UpstreamRouteRuleEntry}
import uk.ac.warwick.tabula.data.{UpstreamRouteRuleDao, UpstreamRouteRuleDaoComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class UpstreamRouteRuleServiceTest extends TestBase with Mockito {

	trait Fixture {
		val route: Route = Fixtures.route("a100")
		val academicYear = AcademicYear(2014)
		val yearOfStudy = 2
		val allModulesList = new UpstreamModuleList(UpstreamModuleList.AllModulesListCode, null, null, 0)
		val otherModulesList = new UpstreamModuleList("List", null, null, 0)

		val service = new AbstractUpstreamRouteRuleService with UpstreamRouteRuleDaoComponent {
			override val upstreamRouteRuleDao: UpstreamRouteRuleDao = smartMock[UpstreamRouteRuleDao]
		}
	}

	@Test
	def normalLoadNoRules(): Unit = {
		new Fixture {

			service.upstreamRouteRuleDao.list(route, academicYear, yearOfStudy) returns Seq()
			val result: Option[BigDecimal] = service.findNormalLoad(route, academicYear, yearOfStudy)
			result.isEmpty should be {true}
		}
	}

	@Test
	def normalLoadNoRelevantRules(): Unit = {
		new Fixture {
			val rule = new UpstreamRouteRule(Some(academicYear), route, yearOfStudy)
			rule.entries.add(new UpstreamRouteRuleEntry(rule, otherModulesList, Option(BigDecimal(120)), None, None, None))
			rule.entries.add(new UpstreamRouteRuleEntry(rule, allModulesList, None, None, None, None))
			service.upstreamRouteRuleDao.list(route, academicYear, yearOfStudy) returns Seq(rule)
			val result: Option[BigDecimal] = service.findNormalLoad(route, academicYear, yearOfStudy)
			result.isEmpty should be {true}
		}
	}

	@Test
	def normalLoadMaxOfRules(): Unit = {
		new Fixture {
			val rule = new UpstreamRouteRule(Some(academicYear), route, yearOfStudy)
			rule.entries.add(new UpstreamRouteRuleEntry(rule, allModulesList, Option(BigDecimal(90)), None, None, None))
			rule.entries.add(new UpstreamRouteRuleEntry(rule, allModulesList, Option(BigDecimal(120)), None, None, None))
			service.upstreamRouteRuleDao.list(route, academicYear, yearOfStudy) returns Seq(rule)
			val result: Option[BigDecimal] = service.findNormalLoad(route, academicYear, yearOfStudy)
			result.nonEmpty should be {true}
			result.get.intValue should be (120)
		}
	}
}