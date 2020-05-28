package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.TestBase

class ComponentScalingCommandTest extends TestBase {

  val scaling: ComponentScalingAlgorithm with ComponentScalingRequest = new ComponentScalingAlgorithm with ComponentScalingRequest

  @Test
  def defaults(): Unit = {
    scaling.scale(0) should be (0)
    scaling.scale(5) should be (6)
    scaling.scale(10) should be (11)
    scaling.scale(15) should be (17)
    scaling.scale(20) should be (23)
    scaling.scale(25) should be (29)
    scaling.scale(30) should be (34)
    scaling.scale(35) should be (40)
    scaling.scale(40) should be (45)
    scaling.scale(45) should be (50)
    scaling.scale(50) should be (55)
    scaling.scale(55) should be (60)
    scaling.scale(60) should be (65)
    scaling.scale(65) should be (70)
    scaling.scale(70) should be (74)
    scaling.scale(75) should be (79)
    scaling.scale(80) should be (83)
    scaling.scale(85) should be (87)
    scaling.scale(90) should be (91)
    scaling.scale(95) should be (96)
    scaling.scale(100) should be (100)
  }

  @Test
  def options(): Unit = {
    scaling.passThreshold = 50
    scaling.lowerBound = 17
    scaling.upperBound = 12

    scaling.scale(0) should be (0)
    scaling.scale(5) should be (8)
    scaling.scale(10) should be (15)
    scaling.scale(15) should be (23)
    scaling.scale(20) should be (30)
    scaling.scale(25) should be (38)
    scaling.scale(30) should be (45)
    scaling.scale(35) should be (52)
    scaling.scale(40) should be (56)
    scaling.scale(45) should be (60)
    scaling.scale(50) should be (64)
    scaling.scale(55) should be (68)
    scaling.scale(60) should be (75)
    scaling.scale(65) should be (78)
    scaling.scale(70) should be (81)
    scaling.scale(75) should be (84)
    scaling.scale(80) should be (88)
    scaling.scale(85) should be (91)
    scaling.scale(90) should be (94)
    scaling.scale(95) should be (97)
    scaling.scale(100) should be (100)
  }

}
