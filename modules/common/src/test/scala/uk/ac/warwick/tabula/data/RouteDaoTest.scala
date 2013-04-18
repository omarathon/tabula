package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Fixtures

class RouteDaoTest extends AppContextTestBase {
	
	lazy val dao = Wire[RouteDao]
	
	@Test def crud = transactional { tx =>
		val route = Fixtures.route("g503")
		dao.saveOrUpdate(route)
		
		dao.getByCode("g503") should be (Some(route))
		dao.getByCode("wibble") should be (None)
	}

}