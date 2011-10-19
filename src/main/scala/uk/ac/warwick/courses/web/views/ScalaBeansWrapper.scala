package uk.ac.warwick.courses.web.views

/*
 * Taken from http://code.google.com/p/sweetscala
 */

import freemarker.ext.beans.{ BeansWrapper }
import freemarker.ext.util.{ ModelCache, ModelFactory }
import freemarker.template.{ ObjectWrapper, SimpleObjectWrapper, TemplateHashModel, TemplateModel, 
  TemplateException, TemplateModelException }
import scala.collection.{ mutable }
import java.{ util=>jutil }
import freemarker.template.DefaultObjectWrapper

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
      //case sobj: ScalaObject => new ScalaHashModel(this, sobj)
      case _ => super.wrap(obj)
    }     
  }
}

/** A model that will expose all Scala getters that has zero parameters
 * to the FM Hash#get method so can retrieve it without calling with parenthesis. */
/*
class ScalaHashModel(wrapper: ObjectWrapper, sobj: ScalaObject) extends TemplateHashModel{
  type Getter = () => AnyRef
  
  val gettersCache = new mutable.HashMap[Class[_], mutable.HashMap[String, Getter]] 
  
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
              map += Pair(n, (() => m.invoke(sobj, Array())))
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
*/


/** Convert scala.Collection to java.util.Collection.
 * Note that java's iterator' remove method will not be implemented. */
class JCollection[A](col: scala.Collection[A]) extends java.util.AbstractCollection[A]{
  def iterator = new java.util.Iterator[A]{
    val elems = col.elements
    def hasNext = elems.hasNext
    def next = elems.next
    def remove = throw new UnsupportedOperationException("remove")
  }
  def size = col.size  
}
/** Campanion class */
object JCollection{
  def apply[A](col: scala.Collection[A]) = new JCollection(col)
}

/** Convert scala.Seq to java.util.List */
class JList[A](seq: scala.Seq[A]) extends java.util.AbstractList[A] {
  def get(idx: Int) = seq(idx)
  def size = seq.size
}
/** Campanion class */
object JList{
  //def apply[A](seq: scala.Seq[A]) = new JList(seq)
  def apply[A](tuples: (A)*) = new JList(tuples)
}

/** A shorter name for java.util.HashMap, but has a campanion object factory. */
class JMap[A,B] extends java.util.HashMap[A,B]
/*class JMap[A](map: scala.collection.Map[A, B]) extends java.util.AbstractMap[A, B] {
  def entrySet: java.utilSet = {
    new JCollection(map
  }
}*/
/** Campanion class the produce java map instance. */
object JMap{
  def apply[A, B](tuples: (A,B)*): JMap[A,B] = {
    val map = new JMap[A,B]
    for((k,v) <- tuples) map.put(k,v)
    map
  }
  //TODO: we should look more on java.util.AbstractMap to actually implement this like JList.
  def apply[A, B](imap: scala.collection.Map[A,B]): JMap[A,B] = {
    val map = new JMap[A,B]
    for((k,v) <- imap) map.put(k,v)
    map 
  }
}
