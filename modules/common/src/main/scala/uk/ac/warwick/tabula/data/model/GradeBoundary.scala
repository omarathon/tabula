package uk.ac.warwick.tabula.data.model

import javax.persistence.Entity
import javax.validation.constraints.NotNull

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
