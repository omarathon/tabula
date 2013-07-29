package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import org.junit.Before

class RouteDaoTest extends PersistenceTestBase {

	val dao = new RouteDaoImpl

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def crud = transactional { tx =>
		val route = Fixtures.route("g503")
		dao.saveOrUpdate(route)

		dao.getByCode("g503") should be (Some(route))
		dao.getByCode("wibble") should be (None)
	}

}