package uk.ac.warwick.courses
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext.xml", "/WEB-INF/*-context.xml"))
@TransactionConfiguration()
@ActiveProfiles(Array("test"))
abstract class AppContextTestBase {

}