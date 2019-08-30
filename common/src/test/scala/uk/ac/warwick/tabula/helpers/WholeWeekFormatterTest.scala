package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.{WeekRange, DayOfWeek}

class WholeWeekFormatterTest extends TestBase {

  @Test def termNumbering() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WholeWeekFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, week 1")
    formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, week 7")
    formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Christmas vacation, w/c Mon 2ⁿᵈ Jan 2012")
    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Easter vacation, w/c Mon 19ᵗʰ Mar 2012")

    formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, week 1")
    formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be("Term 1, weeks 1-2")
    formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = false) should be(
      "Term 1, weeks 1-10; Christmas vacation, w/c Mon 12ᵗʰ Dec 2011 - w/c Mon 2ⁿᵈ Jan 2012"
    )

    formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("1")
    formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("7")
    formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("02/01")
    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term, short = true) should be("19/03")
  }

  @Test def cumulativeTermNumbering() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WholeWeekFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(4)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, week 4")
    formatter.format(Seq(WeekRange(10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, week 10")
    formatter.format(ranges = Seq(WeekRange(13)), dayOfWeek = DayOfWeek.Tuesday, numberingSystem = WeekRange.NumberingSystem.Cumulative, short = false) should be("Christmas vacation, w/c Mon 26ᵗʰ Dec 2011")
    formatter.format(Seq(WeekRange(15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 2, week 11")
    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Easter vacation, w/c Mon 19ᵗʰ Mar 2012")

    formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, week 1")
    formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be("Term 1, weeks 1-2")

    formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = false) should be(
      "Term 1, weeks 1-10; Christmas vacation, w/c Mon 12ᵗʰ Dec 2011 - w/c Mon 2ⁿᵈ Jan 2012"
    )

    formatter.format(Seq(WeekRange(4)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("4")
    formatter.format(Seq(WeekRange(10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("10")
    formatter.format(Seq(WeekRange(13)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("26/12")
    formatter.format(Seq(WeekRange(15)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("11")
    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative, short = true) should be("19/03")
  }


  @Test def academicWeekNumbering() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WholeWeekFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, week 1")
    formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, week 7")
    formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 2, week 16")
    formatter.format(Seq(WeekRange(-8)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Pre-term vacation, week -8")
    formatter.format(Seq(WeekRange(44)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Summer vacation, week 44")
    formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Christmas vacation, week 14")
    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Easter vacation, week 25")

    formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, week 1")
    formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, weeks 1-2")
    formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = false) should be("Term 1, weeks 1-10; Christmas vacation, weeks 11-14")

    formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("1")
    formatter.format(Seq(WeekRange(7)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("7")
    formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("16")
    formatter.format(Seq(WeekRange(-8)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("-8")
    formatter.format(Seq(WeekRange(44)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("44")
    formatter.format(Seq(WeekRange(14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("14")
    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic, short = true) should be("25")
  }

  @Test def noWeekNumbers() = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
    val formatter = new WholeWeekFormatter(AcademicYear.now())

    formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 3ʳᵈ Oct 2011")

    formatter.format(Seq(WeekRange(5)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 31ˢᵗ Oct 2011")

    formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 16ᵗʰ Jan 2012")

    formatter.format(Seq(WeekRange(-8)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 1ˢᵗ Aug 2011")

    formatter.format(Seq(WeekRange(44)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 30ᵗʰ Jul 2012")

    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 19ᵗʰ Mar 2012")

    formatter.format(Seq(WeekRange(1, 1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 3ʳᵈ Oct 2011")

    formatter.format(Seq(WeekRange(1, 2)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be("w/c Mon 3ʳᵈ - w/c Mon 10ᵗʰ Oct 2011")

    formatter.format(Seq(WeekRange(1, 14)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = false) should be(
      "w/c Mon 3ʳᵈ Oct 2011 - w/c Mon 2ⁿᵈ Jan 2012"
    )

    formatter.format(Seq(WeekRange(1)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("03/10")
    formatter.format(Seq(WeekRange(5)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("31/10")
    formatter.format(Seq(WeekRange(16)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("16/01")
    formatter.format(Seq(WeekRange(-8)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("01/08")
    formatter.format(Seq(WeekRange(44)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("30/07")
    formatter.format(Seq(WeekRange(25)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.None, short = true) should be("19/03")

  }


}
