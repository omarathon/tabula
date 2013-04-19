package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.TemplateModel
import freemarker.template.SimpleHash
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.Fixtures
import freemarker.template.TemplateDirectiveBody
import freemarker.core.Environment
import freemarker.template.Template
import java.io.StringWriter
import java.io.StringReader
import uk.ac.warwick.tabula.MockUserLookup
import freemarker.template.utility.DeepUnwrap
import scala.collection.JavaConverters._

class UserLookupTagTest extends TestBase with Mockito {

	val tag = new UserLookupTag
	
	val userLookup = new MockUserLookup
	tag.userLookup = userLookup
	
	userLookup.registerUsers("cuscav", "cusebr")
	
	@Test def singleId = withUser("cuscav") {
		val dept = Fixtures.department("in")
		
		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash
		model.put("id", "cuscav")
		
		val writer = new StringWriter
		
		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]
		
		val params = new java.util.HashMap[String, TemplateModel]
		params.put("id", model.get("id"))
				
		tag.execute(env, params, null, body)
		
		there was one(body).render(writer)
		
		DeepUnwrap.unwrap(env.getCurrentNamespace().get("returned_user")) should be (userLookup.getUserByUserId("cuscav"))
	}
	
	@Test def multipleIds = withUser("cuscav") {
		val dept = Fixtures.department("in")
		
		// Use a SimpleHash as a workaround to wrapping things manually
		val ids: JList[String] = JArrayList()
		ids.add("cuscav")
		ids.add("cusebr")
		
		val model = new SimpleHash
		model.put("ids", ids)
		
		val writer = new StringWriter
		
		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]
		
		val params = new java.util.HashMap[String, TemplateModel]
		params.put("ids", model.get("ids"))
				
		tag.execute(env, params, null, body)
		
		there was one(body).render(writer)
		
		DeepUnwrap.unwrap(env.getCurrentNamespace().get("missing_ids")) should be (Seq())
		DeepUnwrap.unwrap(env.getCurrentNamespace().get("returned_users")) should be (Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		).asJava)
	}
	
}