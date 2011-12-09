package uk.ac.warwick.courses.web.views

/*
 * Taken from http://code.google.com/p/sweetscala
 */

import freemarker.ext.beans.{ BeansWrapper }
import freemarker.ext.util.{ ModelCache, ModelFactory }
import freemarker.template._
import scala.collection.mutable
import java.{ util=>jutil }
import freemarker.template.DefaultObjectWrapper
import uk.ac.warwick.courses.helpers.javaconversions._
import scala.util.matching.Regex
import freemarker.ext.beans.BeanModel

/** A implemenation of BeansWrapper that support native Scala basic and collection types
 * in Freemarker template engine. 
 * 
 * Extends from SimpleObjectWrapper instead of ObjectWrapper because we don't need default
 * not found types to create Jython implementation (which ObjectWrapper will do).
 */
class ScalaBeansWrapper extends DefaultObjectWrapper { 
  def wrapByParent(obj: AnyRef) = super.wrap(obj)
  
  override def wrap(obj: Object): TemplateModel = {
    obj match {
      case Some(x:Object) => wrap(x)
      case smap: scala.collection.Map[_,_] => super.wrap(JMap(smap))
      case sseq: scala.Seq[_] => super.wrap(new JList(sseq))
      case scol: scala.Collection[_] => super.wrap(JCollection(scol))
      //case sdt: JDate => super.wrap(sdt.date) //unwrap the JDate instance to java date.
      case directive: TemplateDirectiveModel => wrapByParent(directive)
      case method: TemplateMethodModel => wrapByParent(method)
      case sobj: ScalaObject => new ScalaHashModel(sobj, this)
      case _ => super.wrap(obj)
    }     
  }
}


/** A model that will expose all Scala getters that has zero parameters
 * to the FM Hash#get method so can retrieve it without calling with parenthesis. */

/**
 * Also understands regular JavaBean getters, useful when a Java bean has been extended
 * in Scala to implement ScalaObject. If both getter type is present, one will overwrite
 * the other in the map but this doesn't really matter as they do the same thing
 */
class ScalaHashModel(sobj: ScalaObject, wrapper: ScalaBeansWrapper) extends BeanModel(sobj,wrapper) {
  type Getter = () => AnyRef
  
  val gettersCache = new mutable.HashMap[Class[_], mutable.HashMap[String, Getter]] 
  
  def lowercaseFirst(camel:String) = camel.head.toLower + camel.tail
  
  val getters = {
    val cls = sobj.getClass
    gettersCache.synchronized{
      gettersCache.get(cls) match {
        case Some(cachedGetters) => cachedGetters
        case None =>{
          val map = new mutable.HashMap[String, Getter]
          cls.getMethods.foreach { m =>
            val n = m.getName
            if(!n.endsWith("_$eq") && m.getParameterTypes.length==0){
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
  override def get(key: String) : TemplateModel = getters.get(key) match {
    case Some(getter) => wrapper.wrap(getter())
    case None => throw new TemplateModelException(key+" not found in object "+sobj)
  }
  override def isEmpty = false
}




