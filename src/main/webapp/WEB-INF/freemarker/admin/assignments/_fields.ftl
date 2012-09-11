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
<div class="row-fluid">
<div class="span6">
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

</div> <#-- end span6 -->

<#if newRecord>
	<!-- <div id="assignment-picker" class="alert alert-success"> -->
	<div class="span6 alert alert-success">
		<p>To find an assignment to pre-populate from, just start typing its name.  Assignments within your 
		department will be matched.  Click on an assignment to choose it.</p>
		<@form.labelled_row "prefillAssignment" "Assignment to copy:">
			<@f.hidden id="prefillAssignment" path="prefillAssignment" />
			
			<#if command.prefillAssignment??>
				<#assign pHolder = "${command.prefillAssignment.name} - ${command.prefillAssignment.module.code}">
			</#if>
			
			<input class="assignment-picker-input" type="text" 
				placeholder="${pHolder!''}">
		</@form.labelled_row>
	</div> <!-- span6 -->
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
</#if>

</div> <#-- end row-fluid div -->


<#if features.assignmentMembership>
	<@form.labelled_row "members" "Students">

		<@f.hidden path="upstreamAssignment" id="upstreamAssignment" />
		<@f.hidden path="occurrence" id="occurrence" />

		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>
		<#assign hasMembers=(membersGroup?? && (membersGroup.includeUsers?size gt 0 || membersGroup.excludeUsers?size gt 0)) />
		
		<#if assessmentGroup??>
		
			${assessmentGroup.members.members?size} students enrolled from SITS
			<#if hasMembers>(with adjustments)</#if>
			<a class="btn" id="show-sits-picker">Change link</a> or <a class="btn" id="show-membership-picker">Adjust membership</a>
		
		<#elseif hasMembers>
			
			${membersGroup.includeUsers?size} students enrolled.
			<div><a class="btn" id="show-membership-picker">Adjust membership</a> or <a class="btn" id="show-sits-picker">Link to SITS</a></div>
			
		<#else>
		
			No students have been enrolled.
			<div><a class="btn" id="show-sits-picker">Link to SITS</a> or <a class="btn" id="show-membership-picker">Add users manually</a></div>
		
		</#if>
		
		<div class="row-fluid">
		<div class="span8">
		
		<!-- Picker to select an upstream assessment group (upstreamassignment+occurrence) -->
		<div class="sits-picker">
			<a class="close" data-dismiss="sits-picker">&times;</a>
			Assessment groups for ${command.academicYear.label}
			<#assign upstreamGroupOptions = command.upstreamGroupOptions />

			<#if assessmentGroup??>
			  <a href="#" class="btn sits-picker-option" data-id="" data-occurrence="">Unlink</a>
			</#if>

			<#if upstreamGroupOptions?? && upstreamGroupOptions?size gt 0>
			<#assign showOccurrence=true>
			<table>
				<tr>
					<th>Name</th>
					<th>Members</th>
					<th>CATS</th>
					<#if showOccurrence><th>Cohort</th></#if>
				</tr>
				<#list upstreamGroupOptions as option>
				<tr>
					<td><a href="#" class="sits-picker-option" data-id="${option.assignmentId}" data-occurrence="${option.occurrence}">${option.name}</a></td>
					<td>${option.memberCount}</td>
					<td>${option.cats!'-'}</td>
					<#if showOccurrence><td>${option.occurrence}</td></#if>
				</tr>
				</#list>
			</table>
			<#else>
			No SITS options available.
			</#if>
		</div>
		
		<div class="membership-picker">
			<a class="close" data-dismiss="membership-picker">&times;</a>
			<div>
				<#assign membershipDetails=command.membershipDetails />

				<div class="tabbable">

					<#assign has_members=(membershipDetails?size gt 0) />
					<#assign tab1class=""/>
					<#assign tab2class=""/>
					<#if has_members>
						<#assign tab1class="active"/>
					<#else>
						<#assign tab2class="active"/>
					</#if>

					<#list command.members.includeUsers as _u>
						<input type="hidden" name="includeUsers" value="${_u}">
					</#list>
					<#list command.members.excludeUsers as _u>
						<input type="hidden" name="excludeUsers" value="${_u}">
					</#list>

					<ul class="nav nav-tabs">
						<li class="${tab1class}"><a href="#membership-tab1" data-toggle="tab">Students</a></li>
						<li class="${tab2class}"><a href="#membership-tab2" data-toggle="tab">Add more</a></li>
					</ul>

					<div class="tab-content">

						<div class="tab-pane ${tab1class}" id="membership-tab1">
							<#if membershipDetails?size gt 0>
								<a href="#"
										class="btn disabled refresh-form has-tooltip"
										id="membership-remove-selected"
										<#if assessmentGroup??>title="This will only adjust membership for this assignment in this app. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
										>
									Remove selected
								</a>

								<div class="scroller">
									<table class="table table-bordered table-striped">
										<tr>
											<th>
												<#-- <@form.selector_check_all /> -->
											</th>
											<th>User</th>
											<th>Name</th>
										</tr>
										<#list membershipDetails as item>
											<#assign u=item.user>
												<tr class="membership-item item-type-${item.itemType}"> <#-- item-type-(sits|include|exclude) -->
													<td>
														<#if item.userId??>
															<#--
																TODO checkboxes are currently all named "excludeUsers", relying on the fact that we only
																use the checkboxes for removing users. If we add other options then this will need changing
																and probably the script will need to generate hidden inputs instead. As it is, the checkboxes
																generate the formdata that we want and so we can just submit it.
															-->
															<@form.selector_check_row "excludeUsers" item.userId />
														</#if>
													</td>
													<td>
														<#if item.itemType='include'><i class="icon-plus-sign"></i></#if>
														<#if item.itemType='exclude'><i class="icon-minus-sign"></i></#if>

														<#if item.itemType='exclude' && item.userId??><a class="btn btn-mini restore refresh-form" data-usercode="${item.userId}">Restore</a></#if>

														<#if u.foundUser>
															${u.userId} <#if item.universityId??>(${item.universityId})</#if>
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
												</tr>
										</#list>
									</table>
								</div>
								<#else>
									<p>No students yet.</p>
							</#if>
						</div>

						<div class="tab-pane ${tab2class}" id="membership-tab2">
							<p>
								Type or paste in a list of usercodes or University numbers here then click Add.
								<strong>Is your module in SITS?</strong> It may be better to fix the data there,
								as other University systems won't know about any changes you make here.
							</p>
							<#-- SOON
							<div>
								<a href="#" class="btn"><i class="icon-user"></i> Lookup user</a>
							</div>
							-->
							<textarea name="massAddUsers"></textarea>
							<button id="add-members" class="btn refresh-form">Add</button>
						</div>

					</div>
				</div>


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

			//close buttons on pickers
			$('.sits-picker, .membership-picker').find('.close').click(function(){
				var $close = $(this);
				$('.' + $close.data('dismiss')).hide();
			});
			
			$sitsPicker.find('.sits-picker-option').click(function(e){
				e.preventDefault();
				$('#upstreamAssignment').val( $(e.target).data('id') );
				$('#occurrence').val( $(e.target).data('occurrence') );
				$('#action-input').val('refresh');
				$('#focusOn').val('member-list');
				$form.submit();
			});

			$('#add-members, #membership-remove-selected').click(function(e){
				$('#focusOn').val('member-list');
			});

			// button to unexclude excluded users
			$membershipPicker.find('.restore').click(function(e){
				var $this = $(this);
				$('#focusOn').val('member-list');
				$this.closest('form').append(
					$('<input type=hidden name=includeUsers />').val($this.data('usercode'))
				);
			});

			var refreshForm = function() {
				$('#action-input').val('refresh');
                $form.submit();
			}

			<#-- controller detects action=refresh and does a bind without submit -->
			$('.refresh-form').click(function(e) {
			    e.preventDefault();
				refreshForm();
			});

			$('select#academicYear').change(function(e) {
				refreshForm();
			});
		
			$('#show-sits-picker').click(function(){
				$('.membership-picker').hide();
				$('.sits-picker').toggle();
			});
			$('#show-membership-picker').click(function(){
				$('.sits-picker').hide();
				var $membershipPicker = $('.membership-picker');
				$membershipPicker.toggle();
				// switch straight to "Add more" tab if the group is empty
				if ($membershipPicker.is(":visible") && $membershipPicker.find('.tab-content table').length == 0) {
      		$membershipPicker.find('a[href=#membership-tab2]').click();
				}
			});

			$('.has-tooltip').tooltip();

		});
		</script>

		<script>
			jQuery(function($){

			<#assign focusOn=RequestParameters.focusOn!'' />
			<#if focusOn='member-list'>
				// focusOn=member-list
        $('#show-membership-picker').click();
			</#if>

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

		<#if features.assignmentMembership>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="restrictSubmissions" />
					Only allow enrolled students to submit
				</label>
				<div class="help-block">
					If you use this option, only students defined above as members will be able to
					submit, so make sure that the membership is correct to avoid problems.
				</div>
			</@form.field>
		</#if>

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

		<#if features.extensions>
			<@form.row>
				<@form.label></@form.label>
				<@form.field>
					<label class="checkbox">
						<@f.checkbox path="allowExtensions" id="allowExtensions" />
						Allow extensions
					</label>
				</@form.field>
			</@form.row>
			<div id="request-extension-row">
				<@form.row>
					<@form.label></@form.label>
					<@form.field>
						<label class="checkbox">
							<@f.checkbox path="allowExtensionRequests" />
							Allow students to request extensions
						</label>
						<div class="help-block">
							Students will be able to request extensions for this assignment via the submission page.
						</div>
					</@form.field>
				</@form.row>
			</div>
		</#if>

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
