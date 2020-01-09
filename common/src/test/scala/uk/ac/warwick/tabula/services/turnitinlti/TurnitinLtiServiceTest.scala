package uk.ac.warwick.tabula.services.turnitinlti

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, TestBase}
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Module}

class TurnitinLtiServiceTest extends TestBase {

  private trait Fixture {
    val file = new FileAttachment

    val assignment: Assignment = Fixtures.assignment("1500 word assignment")
    val module: Module = Fixtures.module("ab101", "First year module")
    assignment.module = module
    assignment.academicYear = AcademicYear(2014)
    assignment.id = "12345"
  }

  @Test def testValidFileType(): Unit = {
    new Fixture {
      file.name = "test.doc"
      TurnitinLtiService.validFileType(file) should be (true)
    }
  }

  @Test def testInvalidFileType(): Unit = {
    new Fixture {
      file.name = "test.gif"
      TurnitinLtiService.validFileType(file) should be (false)
    }
  }

  @Test def generatedClassId(): Unit = {
    new Fixture {
      val prefix = "TestModule"
      TurnitinLtiService.classIdFor(assignment, prefix).value should be("TestModule-ab101")
    }
  }

  @Test def generatedAssignmentId(): Unit = {
    new Fixture {
      TurnitinLtiService.assignmentIdFor(assignment).value should be(s"${TurnitinLtiService.AssignmentPrefix}12345")
    }
  }

  @Test def generatedClassName(): Unit = {
    new Fixture {
      TurnitinLtiService.classNameFor(assignment).value should be("AB101 - First year module")
    }
  }

  @Test def generatedLongClassName(): Unit = {
    new Fixture {
      module.code = "ab102"
      module.name = "First year module with a very very long module name that exceeds Turnitin's maximum of 100 characters"
      TurnitinLtiService.classNameFor(assignment).value should be("AB102 - First year module with a very very long module name that exceeds Turnitin's maximum of 100 ")
    }
  }

  @Test def generatedAssignmentName(): Unit = {
    new Fixture {
      TurnitinLtiService.assignmentNameFor(assignment).value should be("12345 (14/15) 1500 word assignment")
    }
  }

  @Test def removeAccent(): Unit = {
    val badString = "orčpžsíáýd"
    TurnitinLtiService.sanitiseForLti(badString) should be("orcpzsiayd")

    val goodString = "kailan"
    TurnitinLtiService.sanitiseForLti(goodString) should be("kailan")

    val halfGoodString = "kái\tlán"
    TurnitinLtiService.sanitiseForLti(halfGoodString) should be("kai lan")
  }

  @Test def removeNonAscii(): Unit = {
    TurnitinLtiService.sanitiseForLti("kái，lán") should be("kai lan")
    TurnitinLtiService.sanitiseForLti("kái·lán") should be("kai lan")
    TurnitinLtiService.sanitiseForLti("kái、lán") should be("kai lan")
  }
}
