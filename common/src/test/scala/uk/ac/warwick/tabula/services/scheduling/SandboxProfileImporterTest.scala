package uk.ac.warwick.tabula.services.scheduling

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportCommandFactorySetup
import uk.ac.warwick.tabula.data.model.{Gender, MemberUserType}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

import scala.concurrent.Future

class SandboxProfileImporterTest extends TestBase with Mockito with ImportCommandFactorySetup {

  val importer = new SandboxProfileImporter
  importer.reasonableAdjustmentsImporter = smartMock[ReasonableAdjustmentsImporter]
  importer.reasonableAdjustmentsImporter.getReasonableAdjustments(any[String]) returns Future.successful(None)

  @Test def itWorks() = withFakeTime(new DateTime(2013, DateTimeConstants.JULY, 4, 11, 27, 54, 0)) {
    val department = Fixtures.department("hom", "History of Music")
    val macs = importer.membershipInfoByDepartment(department)
    macs.size should be(310)

    val mac = macs(0)

    val member = mac.member
    member.universityId should be("4300001")
    member.departmentCode should be("hom")
    member.email should be("R.Davis@tabula-sandbox.warwick.ac.uk")
    member.targetGroup should be("Undergraduate - full-time")
    member.title should be("Miss")
    member.preferredForenames should be("Rachel")
    member.preferredSurname should be("Davis")
    member.position should be("Undergraduate - full-time")
    member.dateOfBirth should be(new LocalDate(1994, DateTimeConstants.MARCH, 11))
    member.usercode should be("hom0001")
    member.startDate should be(new LocalDate(2010, DateTimeConstants.OCTOBER, 4))
    member.endDate should be(new LocalDate(2013, DateTimeConstants.JUNE, 30))
    member.modified should be(new DateTime(2013, DateTimeConstants.JULY, 4, 11, 27, 54, 0))
    member.phoneNumber should be(null)
    member.gender should be(Gender.Female)
    member.alternativeEmailAddress should be(null)
    member.userType should be(MemberUserType.Student)

    val cmds = importer.getMemberDetails(Seq(mac), Map(), importCommandFactory)
    cmds.size should be(1)

    val cmd = cmds(0)
  }

}
