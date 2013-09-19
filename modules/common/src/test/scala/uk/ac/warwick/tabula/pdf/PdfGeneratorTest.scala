package uk.ac.warwick.tabula.pdf

import uk.ac.warwick.tabula.TestBase
import java.io.{ByteArrayOutputStream, ByteArrayInputStream, File, FileOutputStream}
import uk.ac.warwick.tabula.web.views.{TextRenderer, TextRendererComponent}

class PdfGeneratorTest extends TestBase{

	val pdfGenerator = new FreemarkerXHTMLPDFGeneratorComponent with TextRendererComponent{
		def textRenderer:TextRenderer = new TextRenderer {
			def renderTemplate(templateId: String, model: Any): String = {
				templateId match {
					case "minimal"=>minimalXhtml
				}
			}
		}
	}.pdfGenerator

	@Test
	def canRenderXHTML(){
		val baos = new ByteArrayOutputStream()
		pdfGenerator.renderTemplate("minimal",Map(),baos)
		val of = new FileOutputStream(new File("/tmp/test.pdf"))
		val pdfBytes =baos.toByteArray
		of.write(pdfBytes)
		of.close()
		val pdf = new String(pdfBytes)
		pdf should not be(null)
		pdf should include("%PDF-1.4")
	}

	val minimalXhtml =
		"""<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE html
			PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
			"DTD/xhtml1-transitional.dtd">
			<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
			<head>
			<style type="text/css">
				body {font-family: "Helvetica Neue", Helvetica, Arial, sans-serif ;
							color: green
							}
      </style>
			<title>Test document</title>
			</head>
			<body>
			<p>Hello world</p>
			</body>
			</html>
		""".stripMargin
}

