package uk.ac.warwick.courses.helpers


package javaconversions {
	
/** Convert scala.Collection to java.util.Collection.
 * Note that java's iterator' remove method will not be implemented. */
class JCollection[A](col: scala.Iterable[A]) extends java.util.AbstractCollection[A]{
  def iterator = new java.util.Iterator[A]{
    val elems = col.iterator
    def hasNext = elems.hasNext
    def next = elems.next
    def remove = throw new UnsupportedOperationException("remove")
  }
  def size = col.size  
}
/** Campanion class */
object JCollection{
  def apply[A](col: scala.Iterable[A]) = new JCollection(col)
  
  def contains[T](item:T, collection:Any) = collection match {
	  case collection: JCollection[_] => collection.contains(item)
	  case seq: Seq[_] => seq.contains(item)
  }
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

}