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
	<@form.label clazz="radio" checkbox=true>
		<input id="pointType1" name="pointType" type="radio" value="" <#if !command.pointType??>checked</#if>>
		Standard
	</@form.label>
	<@form.label clazz="radio" checkbox=true>
		<@f.radiobutton path="pointType" value="meeting" />
		Meeting
		<a class="use-popover" id="popover-pointType-meeting" data-content="This monitoring point will be marked as 'attended' if there is a record in Tabula of a meeting taking place between the start and end dates"><i class="icon-question-sign"></i></a>
	</@form.label>
</@form.labelled_row>

<div class="pointTypeOption meeting row-fluid">
	<div class="span5">
		<@form.labelled_row "meetingRelationships" "Meeting with">
			<#assign meetingRelationshipsStrings = command.meetingRelationshipsStrings />
			<#list command.dept.displayedStudentRelationshipTypes as relationship>
				<@form.label checkbox=true>
					<input type="checkbox" name="meetingRelationships" id="meetingRelationships-${relationship.urlPart}" value="${relationship.urlPart}" <#if meetingRelationshipsStrings?seq_contains(relationship.urlPart)>checked</#if> />
					${relationship.agentRole?capitalize}
					<a class="use-popover" id="popover-meetingRelationships-${relationship.urlPart}" data-content="This monitoring point will be marked as 'attended' when a meeting record is created or approved by the student's ${relationship.agentRole?capitalize}"><i class="icon-question-sign"></i></a>
				</@form.label>
			</#list>
		</@form.labelled_row>

		<@form.labelled_row "meetingQuantity" "Number of meetings">
			<@f.input path="meetingQuantity" cssClass="input-mini"/>
			<a class="use-popover" id="popover-meetingQuantity" data-content="The student must have this many meetings between the start and end dates in order to meet this monitoring point"><i class="icon-question-sign"></i></a>
		</@form.labelled_row>
	</div>

	<div class="span6">
		<@form.labelled_row path="meetingFormats" label='Meeting formats <a class="use-popover" id="popover-meetingFormats" data-content="Only selected meeting formats will count towards this monitoring point"><i class="icon-question-sign"></i></a>' noescape=true>
			<#assign meetingFormatsStrings = command.meetingFormatsStrings />
			<#list command.allMeetingFormats as format>
				<@form.label checkbox=true>
					<input type="checkbox" name="meetingFormats" id="meetingFormats-${format.code}" value="${format.description}" <#if meetingFormatsStrings?seq_contains(format.description)>checked</#if> />
					${format.description}
				</@form.label>
			</#list>
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
	if ($('form input[name=pointType]').length > 0) {
		var showOptions = function() {
			var value = $('form input[name=pointType]:checked').val();
			$('.pointTypeOption').hide();
			if (value != undefined && value.length > 0) {
				$('.pointTypeOption.' + value).show();
			}
		};
		$('form input[name=pointType]').on('click', showOptions);
		showOptions();
	}
})(jQuery);
</script>