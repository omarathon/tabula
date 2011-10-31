package uk.ac.warwick.courses.system

import org.springframework.beans.factory.FactoryBean
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.springframework.beans.factory.DisposableBean
import java.util.Properties
import javax.sql.DataSource
import scala.reflect.BeanProperty

/**
 * Just creates a pooling data source.
 * 
 * The main reason for the factory was because setProperties seems to
 * overwrite things like the username and password, so this ensures that 
 * it's called first before those things.
 */
class DataSourceFactory extends FactoryBean[DataSource] with DisposableBean {

  @BeanProperty var user:String =_
  @BeanProperty var password:String =_
  @BeanProperty var url:String =_
  @BeanProperty var driverClass:String =_
  @BeanProperty var properties:Properties =_
  
  lazy val ds = {
    val ds = new ComboPooledDataSource
	if (properties != null) {
		ds.setProperties(properties)
	}
    ds.setUser(user)
    ds.setPassword(password)
    ds.setJdbcUrl(url)
    ds.setDriverClass(driverClass)
    ds
  }
  
  override def getObject = ds
  override def getObjectType = classOf[DataSource]
  override def isSingleton = true
  override def destroy = ds.close
  
}