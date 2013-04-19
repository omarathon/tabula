package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.Fixtures

class RouteDaoTest extends AppContextTestBase {
	
	@Autowired var dao:RouteDao =_
	
	@Test def crud = transactional { tx =>
		val route = Fixtures.route("g503")
		dao.saveOrUpdate(route)
		
		dao.getByCode("g503") should be (Some(route))
		dao.getByCode("wibble") should be (None)
	}

}