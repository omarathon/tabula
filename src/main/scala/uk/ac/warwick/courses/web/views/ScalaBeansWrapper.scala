package uk.ac.warwick.courses.web.views

/*
 * Taken from http://code.google.com/p/sweetscala
 */

import freemarker.ext.beans.{ BeansWrapper }
import freemarker.ext.util.{ ModelCache, ModelFactory }
import freemarker.template._
import scala.collection.mutable
import java.{ util => jutil }
import freemarker.template.DefaultObjectWrapper
import scala.util.matching.Regex
import freemarker.ext.beans.BeanModel
import uk.ac.warwick.courses.helpers.Logging
import scala.collection.JavaConverters._

/**
 * A implemenation of BeansWrapper that support native Scala basic and collection types
 * in Freemarker template engine.
 */
class ScalaBeansWrapper extends DefaultObjectWrapper with Logging {

	def superWrap(obj: Object): TemplateModel = {
		super.wrap(obj)
	}

	override def wrap(obj: Object): TemplateModel = {
		obj match {
			case Some(x: Object) => wrap(x)
			case None => null
			//      case long:Long => superWrap(long:JLong)
			case jcol: java.util.Collection[_] => superWrap(jcol)
			case jmap: java.util.Map[_, _] => superWrap(jmap)
			case smap: scala.collection.Map[_, _] => superWrap(mapAsJavaMapConverter(smap).asJava)
			case sseq: scala.Seq[_] => superWrap(seqAsJavaListConverter(sseq).asJava)
			case scol: scala.Collection[_] => superWrap(asJavaCollectionConverter(scol).asJavaCollection)
			case directive: TemplateDirectiveModel => superWrap(directive)
			case method: TemplateMethodModel => superWrap(method)
			case model: TemplateModel => superWrap(model)
			case sobj: ScalaObject => new ScalaHashModel(sobj, this)
			case _ => superWrap(obj)
		}
	}
}

/**
 * A model that will expose all Scala getters that has zero parameters
 * to the FM Hash#get method so can retrieve it without calling with parenthesis.
 */

/**
 * Also understands regular JavaBean getters, useful when a Java bean has been extended
 * in Scala to implement ScalaObject. If both getter type is present, one will overwrite
 * the other in the map but this doesn't really matter as they do the same thing
 */
class ScalaHashModel(sobj: ScalaObject, wrapper: ScalaBeansWrapper) extends BeanModel(sobj, wrapper) {
	type Getter = () => AnyRef

	val gettersCache = new mutable.HashMap[Class[_], mutable.HashMap[String, Getter]]

	def lowercaseFirst(camel: String) = camel.head.toLower + camel.tail

	val getters = {
		val cls = sobj.getClass
		gettersCache.synchronized {
			gettersCache.get(cls) match {
				case Some(cachedGetters) => cachedGetters
				case None => {
					val map = new mutable.HashMap[String, Getter]
					cls.getMethods.foreach { m =>
						val n = m.getName
						if (!n.endsWith("_$eq") && m.getParameterTypes.length == 0) {
							val javaGetterRegex = new Regex("^(is|get)([A-Z]\\w*)")
							val name = n match {
								case javaGetterRegex(isGet, propName) => lowercaseFirst(propName)
								case _ => n
							}
							map += Pair(name, (() => m.invoke(sobj)))
						}
					}
					gettersCache.put(cls, map)
					map
				}
			}
		}
	}
	override def get(key: String): TemplateModel = {
		val x = key
		getters.get(key) match {
			case Some(getter) => wrapper.wrap(getter())
			case None => checkInnerClasses(key)
		}
	}

	def checkInnerClasses(key: String): TemplateModel = {
		try {
			wrapper.wrap(Class.forName(sobj.getClass.getName + key + "$").getField("MODULE$").get(null))
		} catch {
			case e @ (_: ClassNotFoundException | _: NoSuchFieldException) =>
				throw new TemplateModelException(key + " not found in object " + sobj)
		}
	}

	override def isEmpty = false
}

