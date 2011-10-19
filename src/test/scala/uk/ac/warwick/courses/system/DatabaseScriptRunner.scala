package uk.ac.warwick.courses.system
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io._
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator

import javax.sql.DataSource
import resource.managed

/**
 * Takes a DataSource and executes SQL files found on the classpath
 */
class DatabaseScriptRunner(dataSource:DataSource) extends InitializingBean {
    val populator = new ResourceDatabasePopulator
    
    def setScripts(scripts:Array[Resource]) = populator.setScripts(scripts)
    
	def addScript(path:String) = {
	  populator.addScript(new ClassPathResource(path))
	  this
	}
    
	def afterPropertiesSet = {
		val populator = new ResourceDatabasePopulator
		populator.addScript(new ClassPathResource("data.sql"))
		for (connection <- managed(dataSource.getConnection))
			populator.populate(connection)
	}
	
}