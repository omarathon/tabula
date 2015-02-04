package uk.ac.warwick.tabula.data.model

import javax.persistence.Entity
import javax.validation.constraints.NotNull

object GradeBoundary {
	def apply(marksCode: String, grade: String, minimumMark: Int, maximumMark: Int) = {
		val gb = new GradeBoundary()
		gb.grade = grade
		gb.marksCode = marksCode
		gb.minimumMark = minimumMark
		gb.maximumMark = maximumMark
		gb
	}
}

@Entity
class GradeBoundary extends GeneratedId {

	@NotNull
	var marksCode: String = _

	@NotNull
	var grade: String = _

	@NotNull
	var minimumMark: Int = 0

	@NotNull
	var maximumMark: Int = 100

}
