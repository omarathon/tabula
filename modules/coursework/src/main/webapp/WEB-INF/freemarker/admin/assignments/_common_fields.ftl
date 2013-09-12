<#-- 

This section contains the form fields that can apply to a group of
assignments, as well as to an individual one.

If you add a field it should also be added to _common_fields_hidden.ftl
so that they can be passed around between requests.

-->

<#if features.feedbackTemplates && department.feedbackTemplates?has_content>
	<@form.labelled_row "feedbackTemplate" "Feedback template">
		<@f.select path="feedbackTemplate">
			<@f.option value="" label="No template"/>
			<#list department.feedbackTemplates as template>
				<@f.option value="${template.id}" label="${template.name}"/>
			</#list>
		</@f.select>
		<div class="help-block">
			Select the feedback template that will be used for this assignment. Copies of the template will be
			distributed along with student submissions.
		</div>
	</@form.labelled_row>
</#if>

<#if features.markingWorkflows && department.markingWorkflows?has_content>

	<#assign disabled = !(canUpdateMarkingWorkflow!true)>

	<@form.labelled_row "markingWorkflow" "Marking Workflow">
		<@f.select path="markingWorkflow" disabled="${disabled?string}">
			<@f.option value="" label="None"/>
			<#list department.markingWorkflows as markingWorkflow>
				<@f.option value="${markingWorkflow.id}" label="${markingWorkflow.name}"/>
			</#list>
		</@f.select>
		<div class="help-block">
			<#if disabled>
				<span class="warning">You cannot change the marking workflow for this assignment as it already has submissions.</span>
			<#else>
				Select the way in which this assignment will be marked.
			</#if>
		</div>
	</@form.labelled_row>
</#if>

<#if features.collectMarks>
	<@form.labelled_row "collectMarks" "Marks">
		<label class="checkbox">
			<@f.checkbox path="collectMarks" id="collectMarks" />
			Collect marks
		</label>
	</@form.labelled_row>
</#if>

<#if features.summativeFilter>
	<@form.row>
			<@form.label>Credit bearing</@form.label>
			<@form.field>
				<label class="radio">
					<@f.radiobutton path="summative" value="true" />
					Summative (counts towards final mark)
				</label>
				<label class="radio">
					<@f.radiobutton path="summative" value="false" />
					Formative (does not count towards final mark)
				</label>
				<div class="help-block">
					This field only affects feedback reports.
				</div>
			</@form.field>
		</@form.row>
</#if>