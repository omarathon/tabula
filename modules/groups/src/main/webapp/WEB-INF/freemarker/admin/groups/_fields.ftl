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

	<#if features.smallGroupTeachingStudentSignUp || features.smallGroupTeachingRandomAllocation>
		<@form.labelled_row "allocationMethod" "Allocation method">
			<@form.label checkbox=true>
				<@f.radiobutton path="allocationMethod" value="Manual" />
				Manually allocate students to groups
				<a class="use-popover" data-html="true"
			     data-content="No automatic allocation will occur; students must all be manually allocated to groups by administrators">
			   	<i class="icon-question-sign"></i>
			  </a>
			</@form.label>
			<#if features.smallGroupTeachingStudentSignUp>
				<@form.label checkbox=true>
					<@f.radiobutton path="allocationMethod" value="StudentSignUp" />
					Allow students to sign up for groups
					<a class="use-popover" data-html="true"
				     data-content="Students are allowed to sign up for groups while registration is open. Administrators can still assign students to groups">
				   	<i class="icon-question-sign"></i>
				  </a>
				</@form.label>
			</#if>
			<#if features.smallGroupTeachingRandomAllocation>
				<@form.label checkbox=true>
					<@f.radiobutton path="allocationMethod" value="Random" />
					Randomly allocate students to groups
					<a class="use-popover" data-html="true"
				     data-content="Students in the allocation list are randomly assigned to groups. Administrators can still assign students to groups. There may be a delay between students being added to the allocation list and being allocated to a group.">
				   	<i class="icon-question-sign"></i>
				  </a>
				</@form.label>
			</#if>
		</@form.labelled_row>
	</#if>

</fieldset>

<fieldset id="students">
	<legend>Students <small>Select which students should be in this set of groups</small></legend>
	
	<div class="alert alert-success" style="display: none;" data-display="fragment">
	  The membership list for these groups has been updated
	</div>
	
	<@spring.bind path="members">
		<#assign membersGroup=status.actualValue />
	</@spring.bind>
	<#assign hasMembers=(membersGroup?? && (membersGroup.includeUsers?size gt 0 || membersGroup.excludeUsers?size gt 0)) />
	
	<p>There <#if ((membersGroup.includeUsers?size)!0) == 1>is<#else>are</#if> <@fmt.p (membersGroup.includeUsers?size)!0 "student" "students" /></p>
	
	<#include "_students.ftl" />
</fieldset>

<fieldset id="groups">
	<legend>Groups <small>Create and name empty groups and add weekly events for these groups</small></legend>
	
	<div class="alert alert-success" style="display: none;" data-display="fragment">
	  Your groups have been updated
	</div>

	<@spring.bind path="groups">
		<#assign groups=status.actualValue />
	</@spring.bind>
	
	<#assign groupCount = 0 />
	<#assign deletedGroupCount = 0 />
	<#list groups as group>
		<@spring.bind path="groups[${group_index}].delete">
			<#assign deleteGroup=status.actualValue />
		</@spring.bind>
		
		<#if deleteGroup>
			<#assign deletedGroupCount = deletedGroupCount + 1 />
		<#else>
			<#assign groupCount = groupCount + 1 />
		</#if>
	</#list>
	
	<p>
		There <#if groupCount == 1>is<#else>are</#if> <@fmt.p groupCount "group" "groups" />
		<#if deletedGroupCount gt 0>
			(and <@fmt.p deletedGroupCount "group" "groups" /> marked for deletion)
		</#if>
	</p>
	
	<#include "_groups_modal.ftl" />
	
	<div class="striped-section<#if groups?size gt 0> collapsible expanded</#if>">
		<div class="clearfix">
			<div class="btn-group section-manage-button">
			  <button type="button" data-target="#groups-modal" class="btn" data-toggle="modal">
					<#if groups?size gt 0>Edit<#else>Add</#if> groups
				</button>
			</div>
			<h2 class="section-title with-button">Groups</h2>
		</div>
		
		<#if groups?size gt 0>
			<#include "_events.ftl" />
		</#if>
		
	</div>
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
		
		$('[data-display="fragment"]').each(function() {
			var $div = $(this);
			if (window.location.hash && window.location.hash.substring(1) == $div.closest('fieldset').attr('id')) {
				$div.show();
			} else {
				$div.hide();
			}
		});
	});
</script>
</#escape>
