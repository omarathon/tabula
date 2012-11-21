package uk.ac.warwick.tabula.system
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io._
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import javax.sql.DataSource
import resource.managed
import scala.reflect.BeanProperty

/**
 * Takes a DataSource and executes SQL files found on the classpath
 */
class DatabaseScriptRunner extends InitializingBean {
    val populator = new ResourceDatabasePopulator
    
    @BeanProperty var dataSource:DataSource =_
    
    def setScripts(scripts:Array[Resource]) = {
    	populator.setScripts(scripts)
    }
    
	def addScript(path:String) = {
	  populator.addScript(new ClassPathResource(path))
	  this
	}
    
	def afterPropertiesSet = {
//		val populator = new ResourceDatabasePopulator
//		for (script <- scripts)
//			populator.addScript(script)
//		populator.addScript(new ClassPathResource("data.sql"))
		for (connection <- managed(dataSource.getConnection))
			populator.populate(connection)
	}
	
}