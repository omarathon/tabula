<#-- 
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines. 
-->
<#escape x as x?html>

<#-- Set to "refresh" when posting without submitting -->
<input type="hidden" name="action" id="action-input" >

<#-- ignored by controller but used by this FTL to determine what popups to pre-open -->
<input type="hidden" name="focusOn" id="focusOn">

<#macro datefield path label>
<@form.labelled_row path label>
<@f.input path=path cssClass="date-time-picker" />
</@form.labelled_row>
</#macro>
<div class="assignment-columns">
<div class="assignment-left-fields">
<@form.labelled_row "name" "Assignment name">
<@f.input path="name" cssClass="text" />
</@form.labelled_row>

<@datefield path="openDate" label="Open date" />
<@datefield path="closeDate" label="Close date" />

<#if newRecord>

	<@form.labelled_row "academicYear" "Academic year">
		<@f.select path="academicYear" id="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	</@form.labelled_row>
	
<#else>

	<@form.labelled_row "academicYear" "Academic year">
	<@spring.bind path="academicYearString">
	<span class="uneditable-value">${status.value} <span class="hint">(can't be changed)<span></span>
	</@spring.bind>
	</@form.labelled_row>

</#if>

</div> <#-- end assignment-left-fields div -->
<div id="assignment-picker" class="alert alert-success">
	<p>To find an assignment to pre-populate from, just start typing its name.  Assignments within your 
	department will be matched.  Click on an assignment to choose it.</p>
	<@form.labelled_row "prefillAssignment" "Assignment To Copy From">
		<@f.hidden id="prefillAssignment" path="prefillAssignment" />
		<input class="assignment-picker-input" type="text">
	</@form.labelled_row>
</div>
<div style="clear:both;"></div>
<script>
jQuery(function ($) {
    var assignmentPickerMappings;
	$(".assignment-picker-input").typeahead({
		source: function(query, process) {
			$.get("/admin/module/${module.code}/assignments/picker", { searchTerm : query}, function(data) {
				var labels = []; // labels is the list of Strings representing assignments displayed on the screen
				assignmentPickerMappings = {};
				
				$.each(data, function(i, assignment) {
				    var mapKey = assignment.name + " - " + assignment.moduleCode;
					assignmentPickerMappings[mapKey] = assignment.id;
					labels.push(mapKey);
				})
				process(labels);
			});
		},
		updater: function(mapKey) {
			var assignmentId = assignmentPickerMappings[mapKey];
			$("#prefillAssignment").val(assignmentId);
			$(".assignment-picker-input").val(mapKey);		
			$("#action-input").val("refresh");
			$("#addAssignmentCommand").submit();
			return assignmentName;
		},
		minLength:1
	});
});
</script>
</div> <#-- end assignment-columns div-->

<#-- Members picker is pretty hefty so it is in a separate file -->
<#if features.assignmentMembership>
	<#include "assignment_membership_picker.ftl" />
</#if>

<#-- These fields are also used by the batch assignment importer so they are in a separate file. -->
<#include "_common_fields.ftl" />


</#escape>
