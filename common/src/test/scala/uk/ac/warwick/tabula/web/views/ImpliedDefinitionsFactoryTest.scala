package uk.ac.warwick.tabula.web.views

import java.util.Locale

import org.apache.tiles.Definition
import org.apache.tiles.definition.dao.DefinitionDAO
import org.apache.tiles.locale.LocaleResolver
import org.apache.tiles.request.{ApplicationContext, ApplicationResource, Request}
import uk.ac.warwick.tabula.{Mockito, TestBase}

class ImpliedDefinitionsFactoryTest extends TestBase with Mockito {

  val factory = new ImpliedDefinitionsFactory

  val defDao: DefinitionDAO[Locale] = mock[DefinitionDAO[Locale]]
  val localeResolver: LocaleResolver = mock[LocaleResolver]

  factory.setDefinitionDAO(defDao)
  factory.setLocaleResolver(localeResolver)

  localeResolver.resolveLocale(isA[Request]) returns Locale.ENGLISH

  @Test def matchesDefinition(): Unit = {
    val definition = mock[Definition]
    val ctx = mock[Request]

    defDao.getDefinition("def", Locale.ENGLISH) returns definition

    factory.getDefinition("def", ctx) should be(definition)
  }

  @Test def slashPrefix(): Unit = {
    val ctx = mock[Request]

    factory.getDefinition("/def", ctx) should be(null)
  }

  @Test def resolve(): Unit = {
    val ctx = mock[Request]
    val applicationContext = mock[ApplicationContext]
    ctx.getApplicationContext returns applicationContext
    applicationContext.getResource("/WEB-INF/freemarker/my/template.ftl") returns mock[ApplicationResource]

    val baseDefinition = mock[Definition]
    defDao.getDefinition("base", Locale.ENGLISH) returns baseDefinition

    val defin = factory.getDefinition("my/template", ctx)
    defin should not be null
    defin.getAttribute("body").getValue should be("/WEB-INF/freemarker/my/template.ftl")
  }

  @Test def useFtlhIfAvailable(): Unit = {
    val ctx = mock[Request]
    val applicationContext = mock[ApplicationContext]
    ctx.getApplicationContext returns applicationContext
    applicationContext.getResource("/WEB-INF/freemarker/my/template.ftl") returns mock[ApplicationResource]
    applicationContext.getResource("/WEB-INF/freemarker/my/template.ftlh") returns mock[ApplicationResource]

    val baseDefinition = mock[Definition]
    defDao.getDefinition("base", Locale.ENGLISH) returns baseDefinition

    val defin = factory.getDefinition("my/template", ctx)
    defin should not be null
    defin.getAttribute("body").getValue should be("/WEB-INF/freemarker/my/template.ftlh")
  }
}