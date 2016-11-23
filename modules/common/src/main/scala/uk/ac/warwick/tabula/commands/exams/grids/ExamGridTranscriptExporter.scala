package uk.ac.warwick.tabula.commands.exams.grids

import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.{Document, ParagraphAlignment, XWPFDocument}
import org.joda.time.DateTime
import org.openxmlformats.schemas.wordprocessingml.x2006.main.{STHdrFtr, STTblWidth}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.{Course, Route}

import scala.collection.JavaConverters._

object ExamGridTranscriptExporter extends TaskBenchmarking {

	def apply(
		entities: Seq[ExamGridEntity],
		course: Course,
		route: Route,
		isConfidential: Boolean
	): XWPFDocument = {

		val doc = new XWPFDocument()
		doc.createParagraph()

		def renderEntity(entity: ExamGridEntity): Unit = benchmarkTask("renderEntity") {
			val p1 = doc.getLastParagraph
			p1.createRun().setText(DateFormats.NotificationDateOnly.print(DateTime.now))

			doc.createParagraph()

			doc.createParagraph().createRun().setText(s"Dear ${entity.firstName} ${entity.lastName}")

			doc.createParagraph()

			doc.createParagraph().createRun().setText("The board has agreed the following marks for you:")

			entity.years.keys.toSeq.sorted.foreach { yearOfStudy =>
				val year = entity.years(yearOfStudy)
				val moduleTable = doc.createTable(year.moduleRegistrations.size + 2, 3)
				// Set table width
				val moduleTableWidth = moduleTable.getCTTbl.getTblPr.getTblW
				moduleTableWidth.setW(BigInt(8640).bigInteger)
				moduleTableWidth.setType(STTblWidth.DXA)
				// Set column widths using grid
				moduleTable.getCTTbl.addNewTblGrid().addNewGridCol().setW(BigInt(4320).bigInteger)
				moduleTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(2160).bigInteger)
				moduleTable.getCTTbl.getTblGrid.addNewGridCol().setW(BigInt(2160).bigInteger)
				// Add headers
				moduleTable.getRow(0).getCell(0).setText(s"Year $yearOfStudy")
				moduleTable.getRow(0).getCell(0).getParagraphs.get(0).setAlignment(ParagraphAlignment.CENTER)
				moduleTable.getRow(0).getCell(0).setColor("EEEEEE")
				moduleTable.getRow(0).getCell(0).getCTTc.addNewTcPr().addNewGridSpan().setVal(BigInt(3).bigInteger)
				moduleTable.getRow(0).removeCell(2)
				moduleTable.getRow(0).getCtRow.getTcList.remove(2)
				moduleTable.getRow(0).removeCell(1)
				moduleTable.getRow(0).getCtRow.getTcList.remove(1)
				moduleTable.getRow(1).getCell(0).setText("Module name")
				moduleTable.getRow(1).getCell(0).setColor("EEEEEE")
				moduleTable.getRow(1).getCell(1).setText("Percentage")
				moduleTable.getRow(1).getCell(1).setColor("EEEEEE")
				moduleTable.getRow(1).getCell(2).setText("Grade")
				moduleTable.getRow(1).getCell(2).setColor("EEEEEE")
				// Stop rows breaking over pages
				moduleTable.getRows.asScala.foreach(_.setCantSplitRow(true))

				// Add data
				year.moduleRegistrations.zipWithIndex.foreach { case (mr, index) =>
					val row = moduleTable.getRow(index + 2)
					row.getCell(0).setText(s"${mr.module.code.toUpperCase} ${mr.module.name}")
					if (Option(mr.agreedMark).isDefined) {
						row.getCell(1).setText(mr.agreedMark.toPlainString)
						row.getCell(2).setText(mr.agreedGrade)
					} else if (Option(mr.actualMark).isDefined) {
						row.getCell(1).setText(mr.actualMark.toPlainString)
						row.getCell(2).setText(mr.actualGrade)
					}
				}

				doc.createParagraph()
			}

			doc.createParagraph().createRun().setText("<generic well done message>")
			doc.createParagraph().createRun().setText("<details of next year's induction>")
			doc.createParagraph().createRun().setText("<details of resits>")
			doc.createParagraph()
			doc.createParagraph().createRun().setText("Yours sincerely,")
			doc.createParagraph()
			doc.createParagraph()
			doc.createParagraph()
			doc.createParagraph()
			doc.createParagraph().createRun().setText("<name>")
			doc.createParagraph().createRun().setText("<title>")
		}

		def processEntities(remainingEntities: List[ExamGridEntity]): Unit = {
			remainingEntities match {
				case Nil =>
				case entity :: tail =>
					renderEntity(entity)
					if (tail.nonEmpty) {
						val newParagraph = doc.createParagraph()
						newParagraph.setPageBreak(true)
					}
					processEntities(tail)
			}
		}

		processEntities(entities.toList)

		// Set paragraph spacing to after 0.2cm
		benchmarkTask("paragraphSpacing") {
			val allParagraphs = doc.getParagraphs.asScala ++ doc.getTables.asScala.flatMap(_.getRows.asScala.flatMap(_.getTableCells.asScala.flatMap(_.getParagraphs.asScala)))
			allParagraphs.foreach(_.getCTP.addNewPPr().addNewSpacing().setAfter(BigInt(113).bigInteger))
		}

		if (isConfidential) {
			// Create header
			val header = doc.createHeaderFooterPolicy().createHeader(STHdrFtr.DEFAULT)
			val r = header.createParagraph().createRun()
			// Add the image
			r.addPicture(
				getClass.getResourceAsStream("/confidential-a4-watermark.png"),
				Document.PICTURE_TYPE_PNG,
				"confidential-a4-watermark.png",
				Units.toEMU(Units.pixelToPoints(595)),
				Units.toEMU(Units.pixelToPoints(841))
			)
			val drawing = r.getCTR.getDrawingArray(0)
			// Swap the inline image to an anchored image so it can
			val inline = drawing.getInlineArray(0)
			val anchor = drawing.addNewAnchor()
			anchor.setDocPr(inline.getDocPr)
			anchor.setExtent(inline.getExtent)
			anchor.setBehindDoc(true)
			anchor.setGraphic(inline.getGraphic)
			anchor.addNewWrapNone()
			drawing.removeInline(0)
		}

		doc
	}

}
