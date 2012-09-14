<#--

first page of the form to setup a bunch of assignments from SITS.

-->
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign commandName="addAssignmentsCommand"/>
<#assign command=addAssignmentsCommand />

<h1>Setup assignments</h1>

<#assign step=action!'select'/>

<@f.form method="post" id="batch-add-form" action="/admin/department/${command.department.code}/setup-assignments" commandName=commandName cssClass="form-horizontal">

<#if step='select'>

	<h2>Step 1 - choose which assignments to setup</h2>

	<div class="row-fluid">
  <div class="span10">

	<p>Below are all of the assignments defined for this department in SITS, the central system.</p>

	<p>The first thing to do is choose which ones you want to set up to use for assessment.
	Use the checkboxes on the left hand side to choose which ones you want to setup in the coursework submission system,
	and then click Next. Some items (such as those marked "Audit Only") have already been unchecked.
	</p>

<#elseif step='options'>

	<h2>Step 2 - choose options for assignments</h2>

	<div class="row-fluid">
	<div class="span10">
	
	<div id="batch-add-errors">
	<#include "batch_new_validation.ftl" />
	</div>

	<p>
		Now you need to choose how you want these assignments to behave, such as submission dates
		and resubmission behaviour.
	</p>
	<ul>
		<li>Click and drag to select/unselect assignments (or use the checkboxes on the left).</li>
		<li>Click <strong>Set options</strong> to set e-submission and other options for selected assignments.
				You can overwrite the options for an assignment so it might be a good idea to set the most common options with
				all the assignments selected, and then set more specific options for assignments that require it.
		</li>
		<li>Click <strong>Set dates</strong> to set the opening and closing dates for selected assignments.</li>
		<li>
			Once you've set the options for some assignments, you can click one of the <strong>Re-use</strong> buttons
			to quickly apply those same options to some other assignments.
		</li>
	</ul>

</#if>

<input type="hidden" name="action" value="error" /><!-- this is changed before submit -->


<@form.labelled_row "academicYear" "Academic year">
	<#if step="select">
		<@f.select path="academicYear" id="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	<#else>
  	<@f.hidden path="academicYear"/>
  	<span class="uneditable-value">
  	<@spring.bind path="academicYear">${status.actualValue.label}</@spring.bind>
  	</span>
  </#if>
</@form.labelled_row>

<table class="table table-bordered table-striped" id="batch-add-table">
<tr>
	<th>
		<#if step="options">
			<div class="check-all checkbox">
				<label><span class="very-subtle"></span>
					<input type="checkbox" checked="checked" class="collection-check-all use-tooltip" title="Select/unselect all">
				</label>
			</div>
    	</#if>
	</th>
	<th>Module</th>
	<th>Seq</th>
	<th>Assignment name</th>
	<#if step="options">
	<th></th>
	<th></th>
	</#if>
</tr>
<#list command.assignmentItems as item>
<@spring.nestedPath path="assignmentItems[${item_index}]">
<#if step="select" || item.include>

<tr class="itemContainer">
	<td>
		<@f.hidden path="upstreamAssignment" />
		<#if step="select">
			<@f.checkbox path="include" cssClass="collection-checkbox" />
		<#else>
			<@f.hidden path="include" />

			<input type="checkbox" checked="checked" class="collection-checkbox" />
		</#if>
	</td>
	<td class="selectable">
		${item.upstreamAssignment.moduleCode?upper_case}
	</td>
	<td class="selectable">
		${item.upstreamAssignment.sequence}
	</td>
	<td class="selectable">
		${item.upstreamAssignment.name}
	</td>
	<#if step="options">
 	<td class="selectable assignment-editable-fields-cell">
 		<@f.hidden path="openDate" cssClass="open-date-field" />
 		<@f.hidden path="closeDate" cssClass="close-date-field" />
 		<span class="dates-label">
 			<#if form.hasvalue('openDate') && form.hasvalue('closeDate')>
 				${form.getvalue("openDate")} - ${form.getvalue("closeDate")}
 			</#if>
 		</span>
 	</td>
 	<td>
 		<@f.hidden path="optionsId" cssClass="options-id-input" />
 		<span class="options-id-label">
 			<#if form.hasvalue('optionsId')>
 				<#assign optionsIdValue=form.getvalue('optionsId') />
 				<span class="label label-${optionsIdValue}">${optionsIdValue}</span>
 			</#if>
 		</span>
 	</td>
  </#if>
</tr>

<#else>

	<#-- we include the hidden fields of unincluded items below, outside the table -->

</#if>
</@spring.nestedPath>
</#list>
</table>

<#-- Hidden fields for items we unchecked in the first step, just to remember that we unchecked them -->
<#list command.assignmentItems as item>
<@spring.nestedPath path="assignmentItems[${item_index}]">
<#if step!="select" && !item.include>
	<@f.hidden path="upstreamAssignment" />
	<@f.hidden path="include" />
</#if>
</@spring.nestedPath>
</#list>

</div>

<div class="span2">

<#if step='select'>
<button class="btn btn-large btn-primary btn-block" data-action="options">Next</button>
<#elseif step='options'>
<div id="options-buttons">

<button class="btn btn-large btn-primary btn-block" data-action="options">(RE)FRESH!</button>
<button class="btn btn-large btn-primary btn-block" data-action="submit">Next</button>

<div id="selected-count">0 selected</div>
<div id="selected-deselect"><a href="#">Clear selection</a></div>
<#-- options sets -->
<a class="btn btn-primary btn-block" id="set-options-button" data-target="#set-options-modal" href="<@url page="/admin/department/${department.code}/shared-options"/>">
	Set options&hellip;
</a>
<a class="btn btn-primary btn-block" id="set-dates-button" data-target="#set-dates-modal">
	Set dates&hellip;
</a>

<#list command.optionsMap?keys as optionsId>
	<div class="options-button">
		<button class="btn btn-block" data-group="${optionsId}">
			Re-use options
			<span class="label label-${optionsId}">${optionsId}</span>
		</button>
		<div class="options-group">
			<@spring.nestedPath path="optionsMap[${optionsId}]">
				<#-- Include all the common fields as hidden fields -->
				<#include "_common_fields_hidden.ftl" />
			</@spring.nestedPath>
		</div>
	</div>
</#list> 
 
</div>
</#if>

</div>

</div><#-- .row-fluid -->

</@f.form>

<#if step='options'>

	<#-- popup box for 'Set options' button -->
	<div class="modal hide fade" id="set-options-modal" tabindex="-1" role="dialog" aria-labelledby="set-options-label" aria-hidden="true">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
			<h3 id="set-options-label">Set options</h3>
		</div>
		<div class="modal-body">

		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
			<button class="btn btn-primary">Save options</button>
		</div>
	</div>

	<div class="modal hide fade" id="set-dates-modal" tabindex="-1" role="dialog" aria-labelledby="set-options-label" aria-hidden="true">
		<div class="modal-header">

		</div>
		<div class="modal-body">
			<#-- this form is never submitted, it is just here for styling -->
			<form class="form-horizontal">

			<@spring.nestedPath path=commandName>
			<@form.row>
				<@form.label>Open date</@form.label>
				<@form.field>
					<@spring.bind path="defaultOpenDate">
						<input type="text" id="modal-open-date" name="openDate" class="date-time-picker" value="${status.value}">
					</@spring.bind>
				</@form.field>
			</@form.row>

			<@form.row>
				<@form.label>Close date</@form.label>
				<@form.field>
					<@spring.bind path="defaultOpenDate">
						<input type="text" id="modal-close-date" name="closeDate" class="date-time-picker" value="${status.value}">
					</@spring.bind>
				</@form.field>
			</@form.row>
			</@spring.nestedPath>

			</form>
		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
      <button class="btn btn-primary">Set dates</button>
		</div>
	</div>
</#if>

<script type="text/javascript">
//<[![CDATA[
<#include "batch_new_select_js.ftl" />
//]]>
</script>

</#escape>