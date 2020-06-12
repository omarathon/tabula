package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase

import scala.collection.mutable
import scala.jdk.CollectionConverters._

// scalastyle:off magic.number
class LazyMapsTest extends TestBase {

  @Test def lazyMap(): Unit = {
    var valuesCalculated = 0
    val map: mutable.Map[Int, String] = LazyMaps.create { (i: Int) =>
      valuesCalculated += 1
      "String value %d" format (i)
    }

    map(3) should be("String value 3")
    map(20) should be("String value 20")
    map(20) should be("String value 20")
    valuesCalculated should be(2)
  }

  @Test def contains(): Unit = {
    val map: mutable.Map[Int, Int] = LazyMaps.create { (i: Int) => i + 1 }

    // .contains() just delegates to .get() so we'd expect it to get created
    map.contains(1) should be (true)
    map.asJava.containsKey(1) should be (true)
  }

  @Test def keySet(): Unit = {
    val map: mutable.Map[Int, Int] = LazyMaps.create { (i: Int) => i + 1 }

    // keySet is a view onto the Map, so .keySet.contains will delegate to .contains
    map.keySet.contains(1) should be (true)
    map.asJava.keySet().contains(1) should be (true)
  }

  @Test def keysIterator(): Unit = {
    val map: mutable.Map[Int, Int] = LazyMaps.create { (i: Int) => i + 1 }

    map.keysIterator.contains(1) should be (false)
    map.asJava.asScala.keysIterator.contains(1) should be (false)
  }

}
