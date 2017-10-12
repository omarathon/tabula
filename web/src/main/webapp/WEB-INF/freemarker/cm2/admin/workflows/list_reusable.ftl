<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

	<#function route_function dept>
		<#local selectCourseCommand><@routes.cm2.reusableWorkflowsHome dept academicYear /></#local>
		<#return selectCourseCommand />
	</#function>
	<@cm2.departmentHeader "Marking workflows" department route_function academicYear />

<#if copiedWorkflow??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		${copiedWorkflow.name} was added to ${currentYear.toString}.
	</div>
</#if>

<#if deletedWorkflow??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		${deletedWorkflow} was deleted.
	</div>
</#if>

<#if actionErrors??>
	<div class="alert alert-danger">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		${actionErrors}
	</div>
</#if>

<p>
	Marking workflows define the marking method and who the markers are. Create workflows here and use them with one or more assignments.
	If you need to make workflow changes for an existing CM1 assignment you can still <a href="<@routes.coursework.markingworkflowlist department />">manage CM1 workflows</a>.
</p>
<#if isCurrentYear>
	<p>Below is the list of current workflows available. To copy an old workflow, navigate to the relevant academic year and add the workflow to the current year.</p>
</#if>
<#if isCurrentYearOrLater>
	<@bs3form.labelled_form_group>
		<a class="btn btn-primary" href="<@routes.cm2.reusableWorkflowAdd department academicYear />">
			Create workflow
		</a>
	</@bs3form.labelled_form_group>
</#if>

<#if workflows?has_content>
	<#if !isCurrentYear><p>The following workflows relate to the year selected in the main menu. Use the Add to button to copy a workflow to the current academic year.</p></#if>
	<table class="table-sortable table table-striped">
		<thead>
		<tr>
			<th class="sortable">Name</th>
			<th class="sortable">Type</th>
			<th>Marker</th>
			<th></th>
		</tr>
		</thead>
		<tbody>
			<#list workflows as workflow>
				<#assign inUse = workflow.assignments?size &gt; 0 />
				<tr <#if copiedWorkflow?? && workflow.id == copiedWorkflow.id>class="info"</#if>>
					<td>${workflow.name}</td>
					<td>${workflow.workflowType.description}</td>
					<td>
						<#list workflow.markersByRole?keys as role><#compress>
							<strong>${role}: </strong>
							<#assign markers = mapGet(workflow.markersByRole, role) />
							<#list markers as marker>
								${marker.fullName}<#if marker_has_next>, </#if>
							</#list><br />
						</#compress></#list>
					</td>
					<td>
						<#if isCurrentYearOrLater>
							<a class="btn btn-default" href="<@routes.cm2.reusableWorkflowEdit department academicYear workflow/>">Modify</a>
							<a <#if inUse>
									disabled="disabled"
									class="btn btn-default use-tooltip"
									data-toggle=""
									title="You can't delete this marking workflow because <@fmt.p number=workflow.assignments?size singular="assignment is" plural="assignments are" /> using it"
							<#else>
									class="btn btn-default"
									href="<@routes.cm2.reusableWorkflowDelete department academicYear workflow/>"
							</#if>
							>
								Delete
							</a>
						<#else>
							<a class="btn btn-default" href="<@routes.cm2.reusableWorkflowAddToCurrentYear department academicYear workflow/>">Add to ${currentYear.toString}</a>
						</#if>
					</td>
				</tr>
			</#list>
		</tbody>
	</table>
<#else>
	<p>There are no workflows for ${department.name} in ${academicYear.toString}.</p>
</#if>
</#escape>
