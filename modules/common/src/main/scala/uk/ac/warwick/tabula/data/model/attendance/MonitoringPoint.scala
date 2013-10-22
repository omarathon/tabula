package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.persistence._
import javax.validation.constraints.NotNull
import org.joda.time.DateTime
import scala.Array
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.annotations.BatchSize

@Entity
class MonitoringPoint extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "point_set_id")
	var pointSet: AbstractMonitoringPointSet = _

	@OneToMany(mappedBy = "point", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var checkpoints: JList[MonitoringCheckpoint] = JArrayList()
	
	@NotNull
	var name: String = _
	
	var createdDate: DateTime = _
	
	var updatedDate: DateTime = _

	@NotNull
	var validFromWeek: Int = _

	@NotNull
	var requiredFromWeek: Int = _

	var sentToAcademicOffice: Boolean = false

	def isLate(currentAcademicWeek: Int): Boolean = {
		currentAcademicWeek > requiredFromWeek
	}

}