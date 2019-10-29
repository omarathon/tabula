package uk.ac.warwick.tabula.system

import javax.sql.DataSource
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io._
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator

import scala.util.Using

/**
  * Takes a DataSource and executes SQL files found on the classpath
  */
class DatabaseScriptRunner extends InitializingBean {
  val populator = new ResourceDatabasePopulator

  var dataSource: DataSource = _

  def setScripts(scripts: Array[Resource]): Unit = {
    populator.setScripts(scripts: _*)
  }

  def addScript(path: String): DatabaseScriptRunner = {
    populator.addScript(new ClassPathResource(path))
    this
  }

  def afterPropertiesSet(): Unit = {
    //		val populator = new ResourceDatabasePopulator
    //		for (script <- scripts)
    //			populator.addScript(script)
    //		populator.addScript(new ClassPathResource("data.sql"))
    Using.resource(dataSource.getConnection) { connection =>
      populator.populate(connection)
    }
  }

}
