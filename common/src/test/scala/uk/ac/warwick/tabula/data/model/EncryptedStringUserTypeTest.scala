package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase}

class EncryptedStringUserTypeTest extends TestBase with Mockito {

  // Obviously don't use this for real, it's just for the tests
  EncryptedString.encryptionKey = "cZRYXN05wYqqypMEuJSpnWDV9ynnXIiCNecqeLdmg04="

  val userType = new EncryptedStringUserType

  @Test
  def transitive(): Unit = {
    val input = "wibble"
    userType.convertToObject(userType.convertToValue(input)) should be (input)
  }

  @Test
  def differentOnSubsequentEncrypts(): Unit = {
    val encrypted1 = userType.convertToValue("wibble")
    val encrypted2 = userType.convertToValue("wibble")

    encrypted1 should not be encrypted2

    userType.convertToObject(encrypted1) should be (userType.convertToObject(encrypted2))
  }

}
