package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.persistence._
import javax.validation.constraints.NotNull
import org.joda.time.DateTime
import scala.Array
import uk.ac.warwick.tabula.JavaImports._

@Entity
class MonitoringPoint extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "point_set_id")
	var pointSet: AbstractMonitoringPointSet = _

	@OneToMany(mappedBy = "point", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	var checkpoints: JList[MonitoringCheckpoint] = JArrayList()
	
	@NotNull
	var name: String = _
	
	var defaultValue: Boolean = true
	
	var createdDate: DateTime = _
	
	var updatedDate: DateTime = _

	@NotNull
	var week: Int = _

	var sentToAcademicOffice: Boolean = false

}