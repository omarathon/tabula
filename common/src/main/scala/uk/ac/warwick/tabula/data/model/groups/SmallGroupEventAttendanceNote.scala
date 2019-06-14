package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.AttendanceNote

@Entity
@Proxy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("smallGroup")
class SmallGroupEventAttendanceNote extends AttendanceNote {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  var occurrence: SmallGroupEventOccurrence = _

}