package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.TestBase

class UpstreamMemberTest extends TestBase {

  // SITS names come uppercased - check that we reformat various names correctly.
  @Test def nameFormatting {
    val names = Seq("D'Haenens Johansson", "O'Toole", "Calvo-Bado", "Biggins", "MacCallum", "McCartney")
    for (name <- names) {
      val member = new UpstreamMember
      member.firstName = "ROGER"
      member.lastName = name.toUpperCase
      member.asSsoUser.getLastName should be (name)
    }
  }

}
