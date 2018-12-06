package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.MarkingDescriptor
import uk.ac.warwick.tabula.services.AutowiringMarkingDescriptorServiceComponent
import uk.ac.warwick.tabula.system.TwoWayConverter

class MarkingDescriptorConverter extends TwoWayConverter[String, MarkingDescriptor] with AutowiringMarkingDescriptorServiceComponent {
	override def convertRight(source: String): MarkingDescriptor = markingDescriptorService.get(source).orNull

	override def convertLeft(source: MarkingDescriptor): String = source.id
}
