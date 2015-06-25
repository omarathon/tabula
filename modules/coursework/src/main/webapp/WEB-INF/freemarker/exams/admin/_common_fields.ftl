<@form.labelled_row "name" "Exam name">
	<@f.input path="name" cssClass="text" />
</@form.labelled_row>

<@form.labelled_row "" "Academic year">
	<span class="uneditable-value">
		<#if exam??>${exam.academicYear.toString}<#else>${academicYear.toString}</#if>
		<span class="hint">(can't be changed)</span>
	</span>
</@form.labelled_row>

<#if features.markingWorkflows>
	<@form.labelled_row "markingWorkflow" "Marking workflow">
		<#assign disabled = !(canUpdateMarkingWorkflow!true)>
		<#if command.allMarkingWorkflows?has_content>
				<@f.select path="markingWorkflow" disabled=disabled>
					<@f.option value="" label="None"/>
					<#list command.allMarkingWorkflows as markingWorkflow>
						<@f.option value="${markingWorkflow.id}" label="${markingWorkflow.name} (${markingWorkflow.markingMethod.description})"/>
					</#list>
				</@f.select>
		<#else>
			<p>
				<span class="uneditable-value">
					There are no appropriate workflows available. Only single marker workflows can be used for exams.
				</span>
			</p>
		</#if>
		<div class="help-block">
			<#if disabled>
				<span class="warning">You cannot change the marking workflow for this exam.</span>
			<#else>
				Marking workflows define how and by whom the exam will be marked. They are set up for the department by a Departmental Administrator <#if can.do("MarkingWorkflow.Manage", department)><a href="<@routes.markingworkflowlist department />">here</a></#if>.
			</#if>
		</div>
	</@form.labelled_row>
</#if>

<@sits_groups.exams_sits_groups command />

<@form.labelled_row "massAddUsers" "Additional students">
	<textarea name="massAddUsers" rows="3" style="height: 100px;">${command.originalMassAddUsers!""}</textarea>
	<div class="help-block">
		Additional students registered for the exam. Add University IDs, one per line.
	</div>
</@form.labelled_row>