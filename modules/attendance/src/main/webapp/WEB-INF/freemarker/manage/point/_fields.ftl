<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="input-block-level"/>
</@form.labelled_row>

<@form.labelled_row "validFromWeek" "Start">
	<@f.select path="validFromWeek">
		<#list 1..52 as week>
			<@f.option value="${week}"><@fmt.singleWeekFormat week command.academicYear command.dept /></@f.option>
		</#list>
	</@f.select>
	<a class="use-popover" id="popover-validFromWeek" data-content="You cannot mark a point as attended or missed (unauthorised) before its start date"><i class="icon-question-sign"></i></a>
</@form.labelled_row>

<@form.labelled_row "requiredFromWeek" "End">
	<@f.select path="requiredFromWeek">
		<#list 1..52 as week>
			<@f.option value="${week}"><@fmt.singleWeekFormat week command.academicYear command.dept /></@f.option>
		</#list>
	</@f.select>
	<a class="use-popover" id="popover-requiredFromWeek" data-content="A warning will appear for unrecorded attendance after its end date"><i class="icon-question-sign"></i></a>
</@form.labelled_row>

<@form.labelled_row "pointType" "Type">
	<@f.select path="pointType">
		<@f.option value="">Custom</@f.option>
		<@f.option value="meeting">Meeting</@f.option>
	</@f.select>
</@form.labelled_row>

<div class="pointTypeOption meeting row-fluid">
	<div class="span5">
		<@form.labelled_row "meetingRelationships" "Student relationships">
			<#assign meetingRelationshipsStrings = command.meetingRelationshipsStrings />
			<#list command.dept.displayedStudentRelationshipTypes as relationship>
				<@form.label checkbox=true>
					<input type="checkbox" name="meetingRelationships" id="meetingRelationships-${relationship.urlPart}" value="${relationship.urlPart}" <#if meetingRelationshipsStrings?seq_contains(relationship.urlPart)>checked</#if> />
					${relationship.agentRole?capitalize}
				</@form.label>
			</#list>
		</@form.labelled_row>

		<@form.labelled_row "meetingQuantity" "Number of meetings">
			<@f.input path="meetingQuantity" cssClass="input-small"/>
		</@form.labelled_row>
	</div>

	<div class="span6">
		<#-- TAB-1409 spring binding on meetingFormats doesn't work in the 2-way comverter, so don't bind to it -->
		<@form.labelled_row "" "Meeting formats">
			<#assign meetingFormatsStrings = command.meetingFormatsStrings />
			<#list command.allMeetingFormats as format>
				<@form.label checkbox=true>
					<input type="checkbox" name="meetingFormats" id="meetingFormats-${format.code}" value="${format.description}" <#if meetingFormatsStrings?seq_contains(format.description)>checked</#if> />
					${format.description}
				</@form.label>
			</#list>
			<@f.errors path="" cssClass="error" />
		</@form.labelled_row>


	</div>
</div>

<script>
jQuery(function($){
	$('.use-popover').tabulaPopover({
		trigger: 'click',
		container: '#container'
	});
});

(function($) {
	// Show relavant extra options when changing point type
	if ($('form select#pointType').length > 0) {
		var showOptions = function() {
			var value = $('form select#pointType option:selected').val();
			$('.pointTypeOption').hide();
			if (value.length > 0) {
				$('.pointTypeOption.' + value).show();
			}
		};
		$('form select#pointType').on('change', showOptions);
		showOptions();
	}
})(jQuery);
</script>