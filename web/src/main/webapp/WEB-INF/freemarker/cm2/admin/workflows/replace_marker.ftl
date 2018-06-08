<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

<#assign formAction><@routes.cm2.reusableWorkflowReplaceMarker department academicYear workflow /></#assign>

<@cm2.workflowHeader "Replace marker" workflow "in" />

<@f.form
	method="POST"
	class="double-submit-protection"
	action="${formAction}"
	commandName="replaceMarkerCommand"
>
	<@f.errors cssClass="error form-errors" />

	<@bs3form.labelled_form_group path="oldMarker" labelText="Marker to replace">
		<@f.select path="oldMarker" class="form-control" >
			<option value="" disabled selected></option>
			<#list replaceMarkerCommand.allMarkers as marker>
				<option <#if replaceMarkerCommand.oldMarker?? && replaceMarkerCommand.oldMarker == marker.userId>selected </#if>value="${marker.userId}">${marker.firstName} ${marker.lastName}</option>
			</#list>
		</@f.select>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="newMarker" labelText="New marker">
		<@bs3form.flexipicker path="newMarker" placeholder="User name" list=false multiple=false auto_multiple=false />
	</@bs3form.labelled_form_group>

	<#if replaceMarkerCommand.affectedAssignments?has_content>
		<#if workflow.reusable>
			<@bs3form.labelled_form_group path="" labelText="The following assignments use this marking workflow">
				<ul>
					<#list replaceMarkerCommand.affectedAssignments as assignment>
						<li>${assignment.module.code?upper_case} ${assignment.name}</li>
					</#list>
				</ul>
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group path="includeCompleted" labelText="Should the marker be replaced for completed assignments?">
				<@bs3form.checkbox path="includeCompleted">
					<@f.checkbox path="includeCompleted" /> Replace the marker for assginments where marking has finished.
				</@bs3form.checkbox>
			</@bs3form.labelled_form_group>
		</#if>

		<div class="alert alert-info">
			<p>
				Any students <#if workflow.reusable>to the assignments listed above </#if>that have already been released will not be re-released, so the new marker will not be notified that they need to add marks for the students. You should contact the new marker yourself.
			</p>
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" /> I understand that I need to contact the new marker to inform them that they may need to add marks for students.
			</@bs3form.checkbox>
		</div>
	<#else>
		<@f.hidden path="confirm" value="true" />
	</#if>

	<@bs3form.labelled_form_group>
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn btn-default" href="${returnTo}">Cancel</a>
	</@bs3form.labelled_form_group>

</@f.form>

</#escape>