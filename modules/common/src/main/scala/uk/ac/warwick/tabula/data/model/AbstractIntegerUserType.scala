package uk.ac.warwick.tabula.data.model

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.JavaImports._
import scala.reflect.ClassTag

/**
 * Handles a lot of the junk that isn't necessary if all you want to do is
 * convert between a class and a number.
 */
abstract class AbstractIntegerUserType[A <: Object : ClassTag] extends AbstractBasicUserType[A, JInteger] {

	val basicType = StandardBasicTypes.INTEGER

	override def returnedClass = classOf[JInteger]
	override def sqlTypes = Array(Types.INTEGER)

}

/**
 * If a type implements Convertible and provides an implicit factory method in a companion object,
 * then you can extend this to define a UserType in one line.
 *
 * For converting to the integer, it simply uses the implemented Convertible trait to get
 * the value.
 *
 * For converting from the integer, there is no instance available, so it looks for an
 * implicit JInteger=>A factory. One of the places the Scala compiler will look for this
 * is in the companion object for A, so defining an implicit def there will do the job.
 */
abstract class ConvertibleIntegerUserType[A >: Null <: Convertible[JInteger]](implicit factory: JInteger => A, classTag: ClassTag[A]) extends AbstractIntegerUserType[A] {
	final override def convertToValue(obj: A) = obj.value
	final override def convertToObject(value: JInteger) = factory(value)
	final val nullValue = null
	final val nullObject = null
}