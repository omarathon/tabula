package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.TestBase
import org.junit.Before
import java.util.Properties
import uk.ac.warwick.tabula.helpers.ArrayList
import freemarker.core.Environment
import freemarker.template.Template

class UrlMethodModelTest extends TestBase {
	
	val model: UrlMethodModel = new UrlMethodModel
	
	@Before def setup() {
		model.context = "/courses"
		model.toplevelUrl = "https://courses.warwick.ac.uk/"
		model.staticHashes = new Properties
	}
	
	@Test def fn() {
		model.exec(ArrayList("/module/yes", "/")).toString should be ("/module/yes")
		model.exec(ArrayList("/module/yes")).toString should be ("/courses/module/yes")
		model.exec(ArrayList("/module/yes", "/profiles")).toString should be ("/profiles/module/yes")
	}

}