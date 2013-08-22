package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import org.joda.time.DateTime

class RouteDaoTest extends PersistenceTestBase {

	val dao = new RouteDaoImpl

	val route = Fixtures.route("g503")

	val set = new MonitoringPointSet
	set.route = route
	set.createdDate = DateTime.now()
	set.templateName = "Year 1"

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def getByCode = transactional { tx =>
		dao.saveOrUpdate(route)

		dao.getByCode("g503") should be (Some(route))
		dao.getByCode("wibble") should be (None)
	}

	@Test def getMonitoringPointSetWithoutYear {
		transactional { tx =>
			dao.saveOrUpdate(route)
			session.save(set)
			dao.findMonitoringPointSet(route, Some(1)) should be (None)
			dao.findMonitoringPointSet(route, None) should be (Some(set))
		}
	}

	@Test def getMonitoringPointSetWithYear {
		transactional { tx =>
			dao.saveOrUpdate(route)
			set.year = 2
			session.save(set)
			dao.findMonitoringPointSet(route, Some(2)) should be (Some(set))
			dao.findMonitoringPointSet(route, Some(1)) should be (None)
			dao.findMonitoringPointSet(route, None) should be (None)
		}
	}

}