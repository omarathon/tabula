<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />
	<#include "assign_marker_macros.ftl" />
	<div class="deptheader">
		<h1>Create a new assignment</h1>
		<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
	</div>
	<div class="fix-area">
		<#assign actionUrl><@routes.cm2.assignmentmarkerstemplate assignment mode /></#assign>
		<@f.form method="post" action=actionUrl enctype="multipart/form-data" cssClass="dirty-check" commandName="assignMarkersBySpreadsheetCommand">
			<@components.assignment_wizard 'markers' assignment.module false assignment />
			<p class="btn-toolbar">
				<a class="return-items btn btn-default" href="<@routes.cm2.assignmentmarkers assignment mode />" >
					Return to drag and drop
				</a>
			</p>
			<p>You can assign students to markers by uploading a spreadsheet.</p>
			<ol>
				<li>Download a <a href="<@routes.cm2.assignmentmarkerstemplatedownload assignment mode />">template spreadsheet.</a></li>
				<li>Allocate students to markers using the dropdown menu in the marker name column or by typing a markers usercode into the agent_id column. The agent_id field will be updated with the usercode for that marker if you use the dropdown. Any students with an empty agent_id field will have their marker removed, if they have one.</li>
				<li><strong>Save</strong> your updated spreadsheet.</li>
			</ol>
			<@f.errors cssClass="error form-errors" />

			<@bs3form.filewidget
				basename="file"
				labelText="Choose your updated spreadsheet"
				types=fileTypes
				multiple=false
				required=true
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