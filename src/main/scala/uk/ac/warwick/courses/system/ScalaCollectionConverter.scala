package uk.ac.warwick.courses.system

import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair
import org.springframework.core.convert.TypeDescriptor
import java.util.{Map => JMap}
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.JavaConverters.setAsJavaSetConverter

/**
 * Note: I couldn't get this to work so I caved and specified a setter to take a java.util.Map.
 * 
 * Tell Spring how to pass a Java collection from configuration into a Scala class
 * that's expecting a Scala collection type.
 * As we're dealing with conversion we aren't importing any automatic conversion packages,
 * instead using the explicit conversion methods in JavaConverters.
 */
class ScalaCollectionConverter extends GenericConverter {
   
  // types and conversions to make the below neater
  val JMapClass = classOf[JMap[_,_]]
  val SMapClass = classOf[collection.Map[_,_]]
  val MSMapClass = classOf[mutable.Map[_,_]]
  val ISMapClass = classOf[immutable.Map[_,_]]
  
  val convertibleTypes = Set[ConvertiblePair](
      new ConvertiblePair( JMapClass, MSMapClass ),
      new ConvertiblePair( JMapClass, ISMapClass )
  )
  
  override def getConvertibleTypes = convertibleTypes.asJava
  
  override def convert(source:Object, sourceType:TypeDescriptor, targetType:TypeDescriptor):Object = sourceType.getType match {
    case JMapClass => source match {
      case map:JMap[_,_] => targetType.getType match {
        case ISMapClass => Map( map.asScala.toList : _* )
        case SMapClass => map.asScala
      }
    }
  }

}