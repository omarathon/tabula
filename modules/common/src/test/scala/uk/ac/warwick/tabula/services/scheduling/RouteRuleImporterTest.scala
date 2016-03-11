package uk.ac.warwick.tabula.services.scheduling

import org.junit.After
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import uk.ac.warwick.tabula.data.model.UpstreamModuleList
import uk.ac.warwick.tabula.services.{UpstreamModuleListService, CourseAndRouteService}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import collection.JavaConverters._

class RouteRuleImporterTest extends TestBase with Mockito {
	
	val sits = new EmbeddedDatabaseBuilder().addScript("sits-module-lists.sql").build()

	@After def afterTheFeast() {
		sits.shutdown()
	}

	val routeRuleImporter = new RouteRuleImporterImpl
	routeRuleImporter.sits = sits
	RouteRuleImporter.sitsSchema = "public"
	routeRuleImporter.afterPropertiesSet()

	val academicYear = AcademicYear(2014)

	val route1 = Fixtures.route("a100")
	val route2 = Fixtures.route("b100")
	val route3 = Fixtures.route("c100")
	val route4 = Fixtures.route("d100")
	val allRoutes = Seq(route1, route2, route3, route4)

	val moduleList = new UpstreamModuleList("A100-1-14-CAA", academicYear, route1, 1)

	@Test
	def routeRules(): Unit = {
		routeRuleImporter.courseAndRouteService = smartMock[CourseAndRouteService]
		routeRuleImporter.courseAndRouteService.getRoutesByCodes(any[Seq[String]]) returns allRoutes
		routeRuleImporter.upstreamModuleListService = smartMock[UpstreamModuleListService]
		routeRuleImporter.upstreamModuleListService.findByCodes(any[Seq[String]]) returns Seq(moduleList)
		val result = routeRuleImporter.getRouteRules
		result.size should be (3)
		result.flatMap(_.entries.asScala).size should be (4)
		val route1Rule = result.find(_.route == route1).get
		route1Rule.entries.size should be (2)
	}

}
