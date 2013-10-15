package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import org.joda.time.DateTime

class RouteDaoTest extends PersistenceTestBase {

	val dao = new RouteDaoImpl

	val route = Fixtures.route("g553")

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def getByCode = transactional { tx =>
		dao.saveOrUpdate(route)

		dao.getByCode("g553") should be (Some(route))
		dao.getByCode("wibble") should be (None)
	}
}