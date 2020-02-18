package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Fixtures, TestBase}

class ExamQuestionTest extends TestBase {

  private trait Fixture {
    val exam: Exam = Fixtures.exam()
    val question: ExamQuestion = new ExamQuestion(exam, "1")
    val child: ExamQuestion = Fixtures.examQuestion(exam, question, "a")
    val child2: ExamQuestion = Fixtures.examQuestion(exam, question, "b", 10)
    val grandchild: ExamQuestion = Fixtures.examQuestion(exam, child, "i", 1)
    val grandchild2: ExamQuestion = Fixtures.examQuestion(exam, child, "ii", 2)
    val grandchild3: ExamQuestion = Fixtures.examQuestion(exam, child, "iii", 2)
  }

  @Test def name(): Unit = new Fixture {
    question.name should be("1")
    child.name should be("1-a")
    grandchild.name should be("1-a-i")
  }

  @Test def scores(): Unit = new Fixture {
    question.score should be (Some(15))
    child.score should be (Some(5))
    child2.score should be (Some(10))
    grandchild.score should be (Some(1))

    val noMark: ExamQuestion = new ExamQuestion(exam, "1")
    noMark.score should be(None)

    val noMarkChild: ExamQuestion = Fixtures.examQuestion(exam, noMark, "a")
    val noMarkChild2: ExamQuestion = Fixtures.examQuestion(exam, noMark, "b")
    val noMarkChild3: ExamQuestion = Fixtures.examQuestion(exam, noMark, "c")
    noMark.score should be(None)

    noMarkChild2.score = 9000
    noMark.score should be(Some(9000))
  }

  @Test def restrictionNames(): Unit = new Fixture {
    val questions: Seq[ExamQuestion] = Seq(grandchild, grandchild2, grandchild3)
    new ExamQuestionRule(exam, questions, min=0, max=2).description should be ("At most 2 of 1-a-i, 1-a-ii, 1-a-iii")
    new ExamQuestionRule(exam, questions, min=2, max=0).description should be ("At least 2 of 1-a-i, 1-a-ii, 1-a-iii")
    new ExamQuestionRule(exam, questions, min=1, max=2).description should be ("At least 1 and at most 2 of 1-a-i, 1-a-ii, 1-a-iii")
    new ExamQuestionRule(exam, questions, min=2, max=2).description should be ("2 of 1-a-i, 1-a-ii, 1-a-iii")
  }

  @Test def restrictionCheck(): Unit = new Fixture {
    val r: ExamQuestionRule = new ExamQuestionRule(exam, Seq(grandchild, grandchild2, grandchild3), min=1, max=2)
    r.meetsRestrictions(Set(child2)) should be(false)
    r.meetsRestrictions(Set(child2, grandchild)) should be(true)
    r.meetsRestrictions(Set(child2, grandchild, grandchild2)) should be(true)
    r.meetsRestrictions(Set(child2, grandchild, grandchild2, grandchild3)) should be(false)
  }

}
