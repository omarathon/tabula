<#escape x as x?html>
<#-- Set to "refresh" when posting without submitting -->
<input type="hidden" name="action" id="action-input" value="submit" >

<fieldset>

<@form.labelled_row "format" "Type">
	<@f.select path="format" id="format">
		<@f.options items=allFormats itemLabel="description" itemValue="code" />
	</@f.select>
</@form.labelled_row>

<#if newRecord>

	<@form.labelled_row "academicYear" "Academic year">
		<@f.select path="academicYear" id="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	</@form.labelled_row>
	
<#else>

	<@form.labelled_row "academicYear" "Academic year">
		<@spring.bind path="academicYear">
			<span class="uneditable-value">${status.actualValue.label} <span class="hint">(can't be changed)<span></span>
		</@spring.bind>
	</@form.labelled_row>

</#if>

<@form.labelled_row "name" "Set name">
	<@f.input path="name" cssClass="text" />
	<a class="use-popover" data-html="true"
     data-content="Give this set of groups an optional name to distinguish it from any other sets of the same type - eg. Term 1 seminars and Term 2 seminars">
   	<i class="icon-question-sign"></i>
  </a>
</@form.labelled_row>

</fieldset>

<fieldset id="students">
	<legend>Students <small>Select which students should be in this set of groups</small></legend>
	
	<@spring.bind path="members">
		<#assign membersGroup=status.actualValue />
	</@spring.bind>
	<#assign hasMembers=(membersGroup?? && (membersGroup.includeUsers?size gt 0 || membersGroup.excludeUsers?size gt 0)) />
	
	<p>There are <@fmt.p (membersGroup.includeUsers?size)!0 "student" "students" /></p>
	
	<#include "_students.ftl" />
</fieldset>

<fieldset id="groups">
	<legend>Groups <small>Create and name empty groups</small></legend>
	
	<@spring.bind path="groups">
		<#assign groups=status.actualValue />
	</@spring.bind>
	
	<p>There are <@fmt.p groups?size "group" "groups" /></p>
	
	<#include "_groups.ftl" />
</fieldset>

<fieldset id="events">
	<legend>Events <small>Add weekly events for these groups</small></legend>
	
	<#if groups?size gt 0>
	<#--
		<#assign eventCount=0 />
		<#list groups as group>
			<@spring.nestedPath path="groups[${group_index}]">
				<@spring.bind path="events">
					<#assign events=status.actualValue />
				</@spring.bind>
				
				<#assign eventCount = eventCount + events?size />
			</@spring.nestedPath>
		</#list>
	
		<p>There are <@fmt.p eventCount "event" "events" /></p>
	-->
	
		<#include "_events.ftl" />
	<#else>
		<p>Please add at least one group before creating events.</p>
	</#if>
</fieldset>

<fieldset id="allocation">
	<legend>Allocation <small>Allocate students to these groups</small></legend>
	
	<#include "_allocation.ftl" />
</fieldset>

<script type="text/javascript">
	jQuery(function($) {
		<#-- controller detects action=refresh and does a bind without submit -->
		$('.modal.refresh-form').on('hide', function(e) {
			// Ignore events that are something ELSE hiding and being propagated up!
			if (!$(e.target).hasClass('modal')) return;
		
			// Which section are we targeting?
			var section = $(this).closest('fieldset').attr('id') || '';
			
			if (section) {
				var currentAction = $('#action-input').closest('form').attr('action');
				$('#action-input').closest('form').attr('action', currentAction + '#' + section);
			}
		
			$('#action-input').val('refresh');
      $('#action-input').closest('form').submit();
		});
		
		// Open the first modal with an error in it
		$('.modal .error').first().closest('.modal').modal('show');
		
		// repeat these hooks for modals when shown
		$('body').on('shown', '.modal', function() {
			var $m = $(this);
			$m.find('input.lazy-time-picker').tabulaTimePicker();
		});
	});
</script>
</#escape>