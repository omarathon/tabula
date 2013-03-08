package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import org.apache.tiles.context.TilesRequestContext
import org.apache.tiles.definition.dao.DefinitionDAO
import java.util.Locale
import org.mockito.Matchers._
import org.apache.tiles.Definition
import org.apache.tiles.locale.LocaleResolver

class ImpliedDefinitionsFactoryTest extends TestBase with Mockito {
	
	val factory = new ImpliedDefinitionsFactory
	
	val defDao = mock[DefinitionDAO[Locale]]
	val localeResolver = mock[LocaleResolver]
	
	factory.setDefinitionDAO(defDao)
	factory.setLocaleResolver(localeResolver)
	
	localeResolver.resolveLocale(isA[TilesRequestContext]) returns (Locale.ENGLISH)
	
	@Test def matchesDefinition {
		val definition = mock[Definition]
		val ctx = mock[TilesRequestContext]
		
		defDao.getDefinition("def", Locale.ENGLISH) returns (definition)
		
		factory.getDefinition("def", ctx) should be (definition)
	}
	
	@Test def slashPrefix {
		val ctx = mock[TilesRequestContext]
		
		factory.getDefinition("/def", ctx) should be (null)
	}
	
	@Test def resolve {
		val ctx = mock[TilesRequestContext]
		
		val baseDefinition = mock[Definition]
		defDao.getDefinition("base", Locale.ENGLISH) returns (baseDefinition)
		
		val defin = factory.getDefinition("my/template", ctx)
		defin should not be (null)
		defin.getAttribute("body").getValue() should be ("/WEB-INF/freemarker/my/template.ftl")
	}

}