package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase

import org.junit.Test
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import scala.collection.JavaConverters._


class ModuleTest extends TestBase {
	@Test def stripCats {
		Module.stripCats("md101-15") should be ("md101")
		Module.stripCats("md105-5") should be ("md105")
		Module.stripCats("md105-7.5") should be ("md105")
		intercept[IllegalArgumentException] { Module.stripCats("md105") }
	}
	
	@Test def extractCats {
		Module.extractCats("md101-7.5") should be (Some("7.5"))
		Module.extractCats("md101-15") should be (Some("15"))
		Module.extractCats("md101") should be (None)
	}


  @Test
  def hasUnreleasedGroupSetsReturnsTrueIfAtLeastOneSetIsUnreleased(){
    val fullyReleased = new SmallGroupSet
    fullyReleased.releasedToStudents = true
    fullyReleased.releasedToTutors = true

    val partReleased = new SmallGroupSet
    partReleased.releasedToStudents = false
    partReleased.releasedToTutors = true

    val modFullyReleased = new Module()
    modFullyReleased.groupSets = Seq(fullyReleased).asJava
    modFullyReleased.hasUnreleasedGroupSets should be(false)

    val modPartReleased = new Module()
    modPartReleased.groupSets = Seq(fullyReleased,partReleased).asJava

    modPartReleased.hasUnreleasedGroupSets should be (true)
  }
}