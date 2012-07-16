<#-- 
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines. 
-->
<#escape x as x?html>

<#-- Set to "refresh" when posting without submitting -->
<input type="hidden" name="action" id="action-input" >

<#macro datefield path label>
<@form.labelled_row path label>
<@f.input path=path cssClass="date-time-picker" />
</@form.labelled_row>
</#macro>

<@form.labelled_row "name" "Assignment name">
<@f.input path="name" cssClass="text" />
</@form.labelled_row>

<@datefield path="openDate" label="Open date" />
<@datefield path="closeDate" label="Close date" />

<#if newRecord>

	<@form.labelled_row "academicYear" "Academic year">
		<@f.select path="academicYear">
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

<#if features.assignmentMembership>
	<@form.labelled_row "members" "Students">

		<@f.hidden path="upstreamAssignment" id="upstreamAssignment" />
		<@f.hidden path="occurrence" id="occurrence" />

		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>
		<#assign hasMembers=(membersGroup?? && !membersGroup.empty) />
		
		<#if assessmentGroup??>
		
			${assessmentGroup.members.members?size} students enrolled from SITS
			<#if hasMembers>(with adjustments)</#if>
			<a class="btn" id="show-sits-picker">Change link</a> or <a class="btn" id="show-membership-picker">Adjust membership</a>
		
		<#elseif hasMembers>
			
			${membersGroup.includeUsers} students enrolled.
			<div><a class="btn" id="show-membership-picker">Adjust membership</a> or <a class="btn" id="show-sits-picker">Link to SITS</a></div>
			
		<#else>
		
			No students have been enrolled.
			<div><a class="btn" id="show-sits-picker">Link to SITS</a> or <a class="btn" id="show-membership-picker">Add users manually</a></div>
		
		</#if>
		
		<div class="row-fluid">
		<div class="span8">
		
		<#-- Picker to select an upstream assessment group (upstreamassignment+occurrence) -->
		<div class="sits-picker">
			Assessment groups for ${command.academicYear.label}
			<#assign upstreamGroupOptions = command.upstreamGroupOptions />
			<#if upstreamGroupOptions?? && upstreamGroupOptions?size gt 0>
			<#assign showOccurrence=true>
			<table>
				<tr>
					<th>Name</th>
					<th>Members</th>
					<th>Sequence</th>
					<#if showOccurrence><th>Cohort</th></#if>
				</tr>
				<#list upstreamGroupOptions as option>
				<tr>
					<td><a href="#" class="sits-picker-option" data-id="${option.assignmentId}" data-occurrence="${option.occurrence}">${option.name}</a></td>
					<td>${option.memberCount}</td>
					<td>${option.sequence}</td>
					<#if showOccurrence><td>${option.occurrence}</td></#if>
				</tr>
				</#list>
			</table>
			<#else>
			No SITS options available.
			</#if>
		</div>
		
		<div class="membership-picker">
			<div>Membership</div>
			<div>
				<#assign membershipDetails=command.membershipDetails />
				<a href="#" class="btn btn-danger disabled">Remove selected</a>
				<a href="#" class="btn">Paste list of users</a>
				<a href="#" class="btn"><i class="icon-user"></i> Lookup user</a>
				<#if membershipDetails?size gt 0>
				<div class="scroller">
				<table>
					<tr>
						<th><input type="checkbox"></th>
						<th>User</th>
						<th>Name</th>
						<th></th>
					</tr>
				<#list membershipDetails as item>
					<#assign u=item.user>
					<tr class="membership-item item-type-${item.itemType}"> <#-- item-type-(sits|include|exclude) -->
						<td>
							<input type="checkbox">
						</td>
						<td>
							<#if u.foundUser>
								${u.userId}
							<#elseif item.universityId??>
								Unknown (Uni ID ${item.universityId})
							<#elseif item.userId??>
								Unknown (Usercode ${item.userId})
							<#else><#-- Hmm this bit shouldn't ever happen -->
								
							</#if>
						</td>
						<td>
							<#if u.foundUser>
								${u.fullName}
							</#if>
						</td>
						<td>
							
						</td>
					</tr>
				</#list>
				</table>
				</div>
				<#else>
				<p>No students yet.</p>
				</#if>
			</div>
		</div>
		
		</div>
		</div>
		
		<script>
		jQuery(function($){
			var $sitsPicker = $('.sits-picker');
			var $membershipPicker = $('.membership-picker');
			var $form = $sitsPicker.closest('form');
		
			$sitsPicker.hide();
			$membershipPicker.hide();
			
			$sitsPicker.find('.sits-picker-option').click(function(e){
				$('#upstreamAssignment').val( $(e.target).data('id') );
				$('#occurrence').val( $(e.target).data('occurrence') );
				$('#action-input').val('refresh');
				$form.submit();
			});
		
			$('#show-sits-picker').click(function(){
				$('.membership-picker').hide();
				$('.sits-picker').toggle();
			});
			$('#show-membership-picker').click(function(){
				$('.sits-picker').hide();
				$('.membership-picker').toggle();
			});
		});
		</script>
		
	</@form.labelled_row>
</#if>

<#if features.submissions>
	<@form.labelled_row "collectSubmissions" "Submissions">
		<label class="checkbox">
			<@f.checkbox path="collectSubmissions" id="collectSubmissions" />
			Collect submissions
		</label>
	</@form.labelled_row>
	<fieldset id="submission-options">
		<legend>Submission options</legend>
		<#if features.collectMarks>
			<@form.row>
				<@form.label></@form.label>
				<@form.field>				
					<label class="checkbox">
						<@f.checkbox path="collectMarks" />
						Collect marks
					</label>
				</@form.field>	
			</@form.row>
		</#if>
		<@form.row>
			<@form.label></@form.label>
			<@form.field>				
				<label class="checkbox">
					<@f.checkbox path="displayPlagiarismNotice" />
					Show plagiarism notice
				</label>
			</@form.field>	
		</@form.row>
		<@form.row>
			<@form.label></@form.label>
			<@form.field>				
				<label class="checkbox">
					<@f.checkbox path="allowLateSubmissions" />
					Allow submissions after the close date
				</label>
			</@form.field>	
		</@form.row>
		<@form.row>
			<@form.label></@form.label>
			<@form.field>	
				<label class="checkbox">
					<@f.checkbox path="allowResubmission" />
					Allow students to re-submit work
				</label>
				<div class="help-block">
					Students will be able to submit new work, replacing any previous submission.
					Re-submission is only allowed before the close date.
				</div>
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label path="fileAttachmentLimit">Max attachments per submission</@form.label>
			<@form.field>
				<@f.select path="fileAttachmentLimit" cssClass="span1">
					<@f.options items=1..command.maxFileAttachments />
				</@f.select>
			</@form.field>						
		</@form.row>
		
		<@form.row>
			<@form.label path="fileAttachmentTypes">Accepted attachment file types</@form.label>
			<@form.field>
				<@f.errors path="fileAttachmentTypes" cssClass="error" />
				<@f.input path="fileAttachmentTypes"  type="hidden" />
				<script type="text/javascript" src="/static/js/textList.js"></script>
				<script type="text/javascript">
					jQuery(document).ready(function(){
						var textListController = new TextListController('#fileExtensionList', '#fileAttachmentTypes');
						textListController.transformInput = function(text){
							var result = text.replace(new RegExp('\\.', 'g') , '');
							return result.toLowerCase();
						};
						textListController.preventDuplicates = true;
						textListController.init();
					});
				</script>
				<div id="fileExtensionList" class="textBoxListContainer">
					<ul>
						<li class="inputContainer"><input class="text" type="text"></li>
					</ul>
				</div>
				<div class="help-block">
					Enter the file types you would like to allow separated by spaces (e.g. "pdf doc docx"). Only attachments with the extensions specified will be permitted. Leave this field blank to accept attachments with any extension.
				</div>
			</@form.field>						
		</@form.row>
		
		<div>
		<@form.row path="comment">
		  <@form.label for="assignmentComment">Text to show on submission form:</@form.label>
		  	<@form.field>
				<@f.errors path="comment" cssClass="error" />
				<@f.textarea path="comment" id="assignmentComment" />
				<div class="help-block">
					You can make a new paragraph by leaving a blank line (i.e. press Enter twice).
				</div>
			</@form.field>
		</@form.row>
	</fieldset>
	

<#--
	<@form.row>
	<@form.field>
	
		
		
	</@form.field>
	</@form.row>
-->
	
</#if>

</#escape>