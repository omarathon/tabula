package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.AutowiringMarkingDescriptorDaoComponent
import uk.ac.warwick.tabula.data.model._

trait MarkingDescriptorServiceComponent {
	val markingDescriptorService: MarkingDescriptorService
}

trait AutowiringMarkingDescriptorServiceComponent extends MarkingDescriptorServiceComponent {
	override val markingDescriptorService: MarkingDescriptorService = Wire[MarkingDescriptorService]
}

trait MarkingDescriptorService {
	def get(id: String): Option[MarkingDescriptor]

	def getUniversityMarkingDescriptors: Seq[UniversityMarkingDescriptor]

	def getDepartmentMarkingDescriptors(department: Department): Seq[DepartmentMarkingDescriptor]

	def getMarkingDescriptors(department: Department): Seq[MarkingDescriptor]

	def getForMark(mark: Int, department: Department): MarkingDescriptor

	def save(markingDescriptor: MarkingDescriptor)

	def delete(markingDescriptor: MarkingDescriptor)
}

@Service("markingDescriptorService")
class MarkingDescriptorServiceImpl extends MarkingDescriptorService with AutowiringMarkingDescriptorDaoComponent {
	private def dao = markingDescriptorDao

	override def getUniversityMarkingDescriptors: Seq[UniversityMarkingDescriptor] = dao.getMarkingDescriptors

	override def getDepartmentMarkingDescriptors(department: Department): Seq[DepartmentMarkingDescriptor] = dao.getDepartmentMarkingDescriptors(department)

	override def getMarkingDescriptors(department: Department): Seq[MarkingDescriptor] = (toMarkPointMap(getUniversityMarkingDescriptors) ++ toMarkPointMap(getDepartmentMarkingDescriptors(department))).values.toSeq.distinct

	override def getForMark(mark: Int, department: Department): MarkingDescriptor = dao.getForMark(mark, department)

	override def save(markingDescriptor: MarkingDescriptor): Unit = dao.save(markingDescriptor)

	override def delete(markingDescriptor: MarkingDescriptor): Unit = dao.delete(markingDescriptor)

	override def get(id: String): Option[MarkingDescriptor] = dao.get(id)

	private def toMarkPointMap(markingDescriptors: Seq[MarkingDescriptor]): Map[MarkPoint, MarkingDescriptor] = markingDescriptors.flatMap(zipWithMarkPoints).toMap

	private def zipWithMarkPoints(markingDescriptor: MarkingDescriptor): Seq[(MarkPoint, MarkingDescriptor)] = markingDescriptor.markPoints.map(markPoint => markPoint -> markingDescriptor)
}
