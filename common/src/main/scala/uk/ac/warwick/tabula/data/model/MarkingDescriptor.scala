package uk.ac.warwick.tabula.data.model

import javax.persistence._

@Entity
@DiscriminatorColumn(name = "discriminator")
sealed abstract class MarkingDescriptor extends GeneratedId with Serializable {
	@Column(name = "min_mark")
	var minMark: Int = _

	@Column(name = "max_mark")
	var maxMark: Int = _

	@Column(name = "text")
	var text: String = _

	def minMarkPoint_=(markPoint: MarkPoint): Unit = {
		minMark = markPoint.mark
	}

	def maxMarkPoint_=(markPoint: MarkPoint): Unit = {
		maxMark = markPoint.mark
	}

	def minMarkPoint: MarkPoint = MarkPoint.forMark(minMark).getOrElse(throw new IllegalStateException("No mark point for min mark"))

	def maxMarkPoint: MarkPoint = MarkPoint.forMark(maxMark).getOrElse(throw new IllegalStateException("No mark point for max mark"))

	def isForMarkPoint(markPoint: MarkPoint): Boolean = markPoints.contains(markPoint)

	def markPoints: Seq[MarkPoint] = {
		var points: Seq[MarkPoint] = Nil
		var markPoint = Option(minMarkPoint)

		while (markPoint.exists(_.mark <= maxMark)) {
			points = points :+ markPoint.get
			markPoint = markPoint.flatMap(_.next)
		}

		points
	}
}

@Entity
@DiscriminatorValue(value = "U")
class UniversityMarkingDescriptor extends MarkingDescriptor {
}

@Entity
@DiscriminatorValue(value = "D")
class DepartmentMarkingDescriptor extends MarkingDescriptor {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var department: Department = _
}

