package uk.ac.warwick.courses.system
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import collection.JavaConverters._
import org.springframework.core.convert.TypeDescriptor

class ScalaCollectionConverterTest extends TestBase {
	val JMapClass = classOf[java.util.Map[String,String]]
	val MapClass = classOf[collection.immutable.Map[String,String]]
  
	/*
	 * java.util.Map is converted to scala.collection.Map (important not to just say Map,
	 * as that refers to scala.collection.immutable.Map which a Java map generally won't
	 * be converted into (since there's no interface for an immutable Java map).
	 */
	@Test def convertsMap {
	  val input: java.util.Map[String,String] = Map("Name" -> "John").asJava
	  
	  val converter = new ScalaCollectionConverter
	  val output = converter.convert(input, TypeDescriptor.valueOf(JMapClass), TypeDescriptor.valueOf(MapClass))

	  output match {
	    case map:Map[String,String] => {}
	    case a:Any => fail("Wasn't a map, was a " + a.getClass.getName)
	    case _ => fail("Was nothing")
	  }
	}
}