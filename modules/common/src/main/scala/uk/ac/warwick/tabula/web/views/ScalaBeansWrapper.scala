package uk.ac.warwick.tabula.web.views

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
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.system.permissions.Restricted
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.ScopelessPermission
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import java.lang.reflect.Method

/**
 * A implementation of BeansWrapper that support native Scala basic and collection types
 * in Freemarker template engine.
 */
class ScalaBeansWrapper extends DefaultObjectWrapper with Logging {
	
	var securityService = Wire[SecurityService]

	def superWrap(obj: Object): TemplateModel = {
		super.wrap(obj)
	}

	// scalastyle:off
	override def wrap(obj: Object): TemplateModel = {	
		obj match {
			case Some(x: Object) => wrap(x)
			case Some(null) => null
			case None => null
			//      case long:Long => superWrap(long:JLong)
			case jcol: java.util.Collection[_] => superWrap(jcol)
			case jmap: JMap[_, _] => superWrap(jmap)
			case smap: scala.collection.Map[_, _] => superWrap(mapAsJavaMapConverter(smap).asJava)
			case sseq: scala.Seq[_] => superWrap(seqAsJavaListConverter(sseq).asJava)
			case scol: scala.Iterable[_] => superWrap(asJavaCollectionConverter(scol).asJavaCollection)
			case directive: TemplateDirectiveModel => superWrap(directive)
			case method: TemplateMethodModel => superWrap(method)
			case model: TemplateModel => superWrap(model)
			case sobj if isScalaCompiled(sobj) => new ScalaHashModel(obj, this)
			case _ => superWrap(obj)
		}
	}
	// scalastyle:on
	
	private def isScalaCompiled(obj: Any) = Option(obj) match {
		case Some(obj) if (!obj.getClass.isArray) => obj.getClass.getPackage.getName.startsWith("uk.ac.warwick.tabula")
		case _ => false
	}
	
	/**
	 * A model that will expose all Scala getters that has zero parameters
	 * to the FM Hash#get method so can retrieve it without calling with parenthesis.
	 */
	
	/**
	 * Also understands regular JavaBean getters, useful when a Java bean has been extended
	 * in Scala to implement Any. If both getter type is present, one will overwrite
	 * the other in the map but this doesn't really matter as they do the same thing
	 */
	class ScalaHashModel(sobj: Any, wrapper: ScalaBeansWrapper) extends BeanModel(sobj, wrapper) {
		import ScalaHashModel._

		def lowercaseFirst(camel: String) = camel.head.toLower + camel.tail

		def isGetter(m: Getter) = !m.getName.endsWith("_$eq") && m.getParameterTypes.length == 0
		
		val getters = {
			val cls = sobj.getClass
			gettersCache.getOrElseUpdate(cls, generateGetterInformation(cls))
		}

		def generateGetterInformation(cls: Class[_]) = {
			val javaGetterRegex = new Regex("^(is|get)([A-Z]\\w*)")
			def isJavaGetter(m: Method) = javaGetterRegex.pattern.matcher(m.getName).matches
			
			def parse(m: Method, name: String) = {
				val restrictedAnnotation = m.getAnnotation(classOf[Restricted])
				val perms: Seq[Permission] =
					if (restrictedAnnotation != null) restrictedAnnotation.value map { name => Permissions.of(name) }
					else Nil			
				
				(name -> (m, perms))
			}
			
			val scalaPairs = for (m <- cls.getMethods if isGetter(m) && !isJavaGetter(m)) yield {
				parse(m, m.getName)
			}
			val javaPairs = for (m <- cls.getMethods if isGetter(m) && isJavaGetter(m)) yield {
				val name = m.getName match {
					case javaGetterRegex(isGet, propName) => lowercaseFirst(propName)
					case _ => throw new IllegalStateException("Couldn't match java getter syntax")
				}
				parse(m, name)
			}
			
			// TAB-766 Add the scala pairs after so they override the java ones of the same name
			javaPairs.toMap ++ scalaPairs.toMap
		}
		
		// Cache child properties for the life of this model, so that their caches are useful when a property is accessed twice.
		// Not the same as TAB-469, which would cache for longer than the life of a request, causing memory leaks. This cache
		// will use a bit more memory but it won't leak outside of a request.
		var cachedResults = mutable.HashMap[String, TemplateModel]()

		def user = RequestInfo.fromThread.get.user

		def canDo(perms: Seq[Permission]) = perms forall { 
			case permission: ScopelessPermission => securityService.can(user, permission)
			case permission if classOf[PermissionsTarget].isInstance(sobj) => 
				securityService.can(user, permission, sobj.asInstanceOf[PermissionsTarget])
			case permission => {
				logger.warn("Couldn't check for permission %s against object %s because it isn't a PermissionsTarget".format(permission.toString, sobj.toString))
				false
			}
		}
		
		override def get(key: String): TemplateModel = {
			val x = key
			cachedResults.getOrElseUpdate(key, 
				getters.get(key) match {
					case Some((getter, permissions)) => 
						if (canDo(permissions)) wrapper.wrap(getter.invoke(sobj))
						else null
					case None => checkInnerClasses(key) match {
						case Some(method) => method
						case None => super.get(key)
					}
				}
			)
		}
	
		def checkInnerClasses(key: String): Option[TemplateModel] = {
			try {
				Some(wrapper.wrap(Class.forName(sobj.getClass.getName + key + "$").getField("MODULE$").get(null)))
			} catch {
				case e @ (_: ClassNotFoundException | _: NoSuchFieldException) =>
					None
			}
		}
	
		override def isEmpty = false
		
		def clearCaches() {
			cachedResults.clear()
		}
	}
	
	object ScalaHashModel {
		type Getter = java.lang.reflect.Method
		
		val gettersCache = new mutable.HashMap[Class[_], Map[String, (Getter, Seq[Permission])]]
	}

}
