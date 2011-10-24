package uk.ac.warwick.courses.web.views

/*
 * Taken from http://code.google.com/p/sweetscala
 */

import freemarker.ext.beans.{ BeansWrapper }
import freemarker.ext.util.{ ModelCache, ModelFactory }
import freemarker.template._
import scala.collection.{ mutable }
import java.{ util=>jutil }
import freemarker.template.DefaultObjectWrapper
import uk.ac.warwick.courses.helpers.javaconversions._
import scala.util.matching.Regex

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
      case sobj: ScalaObject => new ScalaHashModel(this, sobj)
      case _ => super.wrap(obj)
    }     
  }
}

/*
 * ScalaHashModel is supposed to wrap a ScalaObject to allow Freemarker to use the scala-style
 * getters like .name rather than .getName(). However it seems to fail in for a ScalaObject
 * that doesn't have these getters - most prominently where you've extended a Java object.
 * So I've just removed it for now, as most of our stuff uses @BeanProperty and so has javabean
 * getters on them anyway.
 * 
 * Might be fixable if it can be taught to look for .getX as well as .x
 */

/** A model that will expose all Scala getters that has zero parameters
 * to the FM Hash#get method so can retrieve it without calling with parenthesis. */

class ScalaHashModel(wrapper: ObjectWrapper, sobj: ScalaObject) extends TemplateHashModel{
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
  def get(key: String) : TemplateModel = getters.get(key) match {
    case Some(getter) => wrapper.wrap(getter())
    case None => throw new TemplateModelException(key+" not found in object "+sobj)
  }
  def isEmpty = false
}




