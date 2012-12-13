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
	
	@Test def encoding() {
		val input = "/download/greek \u03a7\u03a8\u03a9.doc"
		val expected = "/courses/download/greek%20%CE%A7%CE%A8%CE%A9.doc"
		model.exec(ArrayList(input)).toString should be (expected)
	}

}