package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.AcademicYear

@Entity
class CourseYearWeighting extends GeneratedId with Ordered[CourseYearWeighting] {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "courseCode", referencedColumnName="code")
	@NotNull
	var course: Course = _

	@Basic
	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = _

	@NotNull
	var yearOfStudy: Int = _

	@NotNull
	var weighting: JBigDecimal = _

	@transient
	private val percentageMultiplier = new JBigDecimal(100)

	def weightingAsPercentage: String = weighting.multiply(percentageMultiplier).stripTrailingZeros.toPlainString + "%"

	def compare(that:CourseYearWeighting): Int = {
		if (this.course.code != that.course.code)
			this.course.code.compare(that.course.code)
		else if (this.academicYear != that.academicYear)
			this.academicYear.compare(that.academicYear)
		else
			this.yearOfStudy.compare(that.yearOfStudy)
	}

}
