<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />
	<#import "*/cm2_macros.ftl" as cm2 />
	<#include "assign_marker_macros.ftl" />

	<@cm2.assignmentHeader "Assign markers" assignment "for" />

	<div class="fix-area">
		<#assign actionUrl><@routes.cm2.assignmentmarkerstemplate assignment mode /></#assign>
		<@f.form method="post" action=actionUrl enctype="multipart/form-data" cssClass="dirty-check" commandName="assignMarkersBySpreadsheetCommand">
			<@components.assignment_wizard 'markers' assignment.module false assignment />
			<p class="btn-toolbar">
				<a class="return-items btn btn-default" href="<@routes.cm2.assignmentmarkers assignment mode />" >
					Return to drag and drop
				</a>
			</p>
			<p>You can assign students to markers in bulk by uploading a spreadsheet containing the allocation:</p>
			<ol>
				<li>Download a <a href="<@routes.cm2.assignmentmarkerstemplatedownload assignment mode />">template spreadsheet.</a></li>
				<li>In the <strong>Marker name</strong> column, choose the marker you wish to allocate from the drop-down menu.
					When you choose a marker, the <strong>Marker usercode</strong> column updates automatically.</li>
				<li>Save your updated spreadsheet as an XLSX file.</li>
				<li>Upload the spreadsheet to Tabula.</li>
			</ol>
			<@f.errors cssClass="error form-errors" />

			<@bs3form.filewidget
				basename="file"
				labelText="Choose your updated spreadsheet"
				types=fileTypes
				multiple=false
				required=true
				customHelp="You must attach one spreadsheet saved as an XLSX file."
			/>

			<div class="fix-footer">
				<input
						type="submit"
						class="btn btn-primary"
						name="preview"
						value="Upload"
				/>
			</div>
		</@f.form>
	</div>
</#escape>