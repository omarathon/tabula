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
	
		<script type="text/javascript">
			jQuery(function($) {
				$('#academicYear').on('change', function(e) {
					var $form = $(this).closest('form');
					$('#action-input').val('refresh');
					
					$form.submit();
				});
			});
		</script>
		
	<#else>
	
		<@form.labelled_row "academicYear" "Academic year">
			<@spring.bind path="academicYear">
				<span class="uneditable-value">${status.actualValue.label} <span class="hint">(can't be changed)</span></span>
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
			<label class="radio">
				<@f.radiobutton path="allocationMethod" value="Manual" />
				Manual allocation
				<a class="use-popover" data-html="true"
			     data-content="Allocate students by drag and drop or spreadsheet upload">
			   	<i class="icon-question-sign"></i>
			  </a>
			</label>
			<#if features.smallGroupTeachingStudentSignUp>
				<label class="radio">
					<@f.radiobutton path="allocationMethod" value="StudentSignUp" selector=".student-sign-up-options" />
					Self sign-up
					<a class="use-popover" data-html="true"
				     data-content="Allow students to sign up for groups (you can edit group allocation later)">
				   	<i class="icon-question-sign"></i>
				  </a>
				</label>
			</#if>
			<#if features.smallGroupTeachingRandomAllocation>
				<label class="radio">
					<@f.radiobutton path="allocationMethod" value="Random" />
					Randomly allocate students to groups
					<a class="use-popover" data-html="true"
				     data-content="Students in the allocation list are randomly assigned to groups. Administrators can still assign students to groups. There may be a delay between students being added to the allocation list and being allocated to a group.">
				   	<i class="icon-question-sign"></i>
				  </a>
				</label>
			</#if>
		</@form.labelled_row>
	</#if>

	<#if features.smallGroupTeachingStudentSignUp>
			<@form.row defaultClass="">
    			<@form.field>
    				<@form.label checkbox=true>
    					<@f.checkbox path="studentsCanSeeTutorName" id="studentsCanSeeTutorName" />
    						Students can see tutor name
    						<a class="use-popover" data-html="true"
    										 data-content="Students can see tutor names when deciding which group to sign up for">
    										<i class="icon-question-sign"></i>
    						</a>
    				</@form.label>
    				<@f.errors path="studentsCanSeeTutorName" cssClass="error" />
    			</@form.field>
    	    </@form.row>
	</#if>
	<#if features.smallGroupTeachingStudentSignUp>
			<@form.row defaultClass="">
    			<@form.field>
    				<@form.label checkbox=true>
    					<@f.checkbox path="studentsCanSeeOtherMembers" id="studentsCanSeeOtherMembers" />
    						Students can see student names
    						<a class="use-popover" data-html="true"
    										 data-content="Students can see the names of any other students in the group when deciding which group to sign up for">
    										<i class="icon-question-sign"></i>
    						</a>
    				</@form.label>
    				<@f.errors path="studentsCanSeeOtherMembers" cssClass="error" />
    			</@form.field>
    	    </@form.row>
	</#if>


	<#if features.smallGroupTeachingSelfGroupSwitching>
		
		<@form.row path="allowSelfGroupSwitching" cssClass="student-sign-up-options">
			<@form.field>
				<@form.label checkbox=true >
					<@f.checkbox path="allowSelfGroupSwitching" />
					Allow students to switch groups
					<a class="use-popover" data-html="true"
						data-content="When self sign up is enabled students will be able to switch groups.">
							<i class="icon-question-sign"></i>
					</a>
				</@form.label>
			</@form.field>
		</@form.row>
	</#if>

</fieldset>

<#import "*/membership_picker_macros.ftl" as membership_picker />

<@form.row "members" "groupEnrolment">
	<details id="students-details">
		<summary id="students-summary" class="collapsible large-chevron">
			<span class="legend" >Students <small>Select which students should be in this set of groups</small> </span>

			<div class="alert alert-success" style="display: none;" data-display="fragment">
				The membership list for these groups has been updated
			</div>

			<@spring.bind path="members">
				<#assign membersGroup=status.actualValue />
			</@spring.bind>
			<#assign hasMembers=(membersGroup?? && (membersGroup.includeUsers?size gt 0 || membersGroup.excludeUsers?size gt 0)) />

			<#-- Members picker is pretty hefty so it is in a separate file -->
			<#if editSmallGroupSetCommand??>
				<#assign command=editSmallGroupSetCommand />
			<#else>
				<#assign command=createSmallGroupSetCommand />
			</#if>
			<@membership_picker.header command />
		</summary>
		<@membership_picker.fieldset command 'group' 'group set'/>
	</details>
</@form.row>

<fieldset id="groups">
	<details id="groups-details">
		<summary class="collapsible large-chevron" id="groups-summary">
			<span class="legend" >Groups <small>Create and name empty groups and add weekly events for these groups</small> </span>

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
				<@fmt.p groupCount "group" "groups" />
				<#if deletedGroupCount gt 0>
					(and <@fmt.p deletedGroupCount "group" "groups" /> marked for deletion)
				</#if>
			</p>

		</summary>

		<@form.row defaultClass="maxGroupSize groupDetail">
			<@form.field>
				<@form.label checkbox=true>
					<@f.checkbox path="defaultMaxGroupSizeEnabled" id="defaultMaxGroupSizeEnabled" />
					Set maximum group size:
				</@form.label>

				<#if set??>
					<#assign disabled = !(set.defaultMaxGroupSizeEnabled!true)>
				<#else>
					<#assign disabled = "true" >
				</#if>

				<@f.input path="defaultMaxGroupSize" type="number" min="0" max="100" cssClass="input-small" disabled="${disabled?string}" />

				<a class="use-popover" data-html="true"
				   data-content="This is the default maximum size for any new groups you create.  You can adjust the maximum size of individual groups">
					<i class="icon-question-sign"></i>
				</a>
				<@f.errors path="defaultMaxGroupSize" cssClass="error" />
			</@form.field>
		</@form.row>
	
		<div class="striped-section">
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
	</details>
</fieldset>
<#if groups?size gt 0>
	<#include "_events_modals.ftl" />
</#if>
<#include "_groups_modal.ftl" />

<script type="text/javascript">

	jQuery(function($) {

		$('#action-input').closest('form').on('click', '.update-only', function() {
			$('#action-input').val('update');
		});

		var setMaxSizeOptions = function() {
			if ($("#defaultMaxGroupSizeEnabled:checked").val()){
				$('#defaultMaxGroupSize').removeAttr('disabled');
				$(".groupSizeUnlimited").hide();
				$(".groupSizeLimited").show();
			} else {
				$('#defaultMaxGroupSize').attr('disabled', 'disabled');
				$(".groupSizeUnlimited").show();
				$(".groupSizeLimited").hide();
			}
		}

		setMaxSizeOptions();

		$('#defaultMaxGroupSizeEnabled').change(setMaxSizeOptions);

		// Set up radios to enable/disable self-sign up options fields.
		$("input:radio[name='allocationMethod']").radioControlled();

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
