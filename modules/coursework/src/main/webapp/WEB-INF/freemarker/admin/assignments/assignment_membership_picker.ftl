<#--

	assignment membership editor split out from _fields.ftl for readability.

-->
<@form.labelled_row "members" "Students">

		<@f.hidden path="upstreamAssignment" id="upstreamAssignment" />
		<@f.hidden path="occurrence" id="occurrence" />

		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>
		<#assign hasMembers=(membersGroup?? && (membersGroup.includeUsers?size gt 0 || membersGroup.excludeUsers?size gt 0)) />

		<#macro what_is_this>
			<a href="#" class="use-popover" 
			   data-title="Student membership"
			   data-trigger="hover"
	   		   data-html="true"
			   data-content="&lt;p&gt;Here you can specify where this assignment should get its list of enrolled students from.
			     You can link to a central SITS assignment and a live list of students will be maintained.
			     If you are not using SITS you can manually specify a list of users.&lt;/p&gt;&lt;p&gt;
			     It is also possible to tweak the membership even when using SITS data, but this is only to be used
			     when necessary and you still need to ensure that the upstream SITS data gets fixed.
			     &lt;/p&gt;"
			   >What's this?</a>
		</#macro>

		<#if upstreamAssessmentGroups?has_content>
			<#assign total=0 />
			<#list upstreamAssessmentGroups as group>
				<#assign total=total+group.members.members?size />
			</#list>
			<#if hasMembers>
				<#assign total=total+membersGroup.includeUsers?size />
			</#if>

			${total} students enrolled from SITS <@what_is_this />
			<#if hasMembers>(with adjustments)</#if>
			<a class="btn" id="show-sits-picker">Change link</a> or <a class="btn" id="show-membership-picker">Adjust membership</a>

		<#elseif hasMembers>

			${membersGroup.includeUsers?size} students enrolled. <@what_is_this />
			<div><a class="btn" id="show-membership-picker">Adjust membership</a> or <a class="btn" id="show-sits-picker">Link to SITS</a></div>

		<#else>

			No students have been enrolled. <@what_is_this />
			<div><a class="btn" id="show-sits-picker">Link to SITS</a> or <a class="btn" id="show-membership-picker">Add users manually</a></div>

		</#if>

		<div class="row-fluid">
		<div class="span8">

		<!-- Picker to select an upstream assessment group (upstreamassignment+occurrence) -->
		<div class="ag-picker">
			<a class="close" data-dismiss="ag-picker">&times;</a>
			<#if linkedAssessmentGroups?has_content>
				<#list linkedAssessmentGroups as group>
					<input class="linked-group"
						   type="hidden"
						   name="assessmentGroups"
						   value="${group.id}"
						   data-id = "${group.upstreamAssignment.id}"
						   data-occurrence = "${group.occurrence!""}" />
				</#list>
			<#else>
				<@f.hidden class="empty-group group-id" name="assessmentGroups" value="" />
			</#if>
			Assessment groups for ${command.academicYear.label}
			<#if command.upstreamGroupOptions?has_content>
				<#assign showOccurrence=true>
				<table>
					<thead>
						<tr>
							<th>Name</th>
							<th>Members</th>
							<th>CATS</th>
							<th>Cohort</th>
							<th>Sequence</th>
						</tr>
					</thead>
					<tbody><#list command.upstreamGroupOptions as option>
						<#assign isLinked = option.linked/>
						<tr class="${isLinked?string('linked', '')}">
							<td>
								<a href="#"
								   class="ag-picker-option"
								   data-id="${option.assignmentId}"
								   data-occurrence="${option.occurrence}">
									${option.name}
								</a>
							</td>
							<td>${option.memberCount}</td>
							<td>${option.cats!'-'}</td>
							<td>${option.occurrence}</td>
							<td>${option.sequence}</td>
						</tr>
					</#list></tbody>
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

					<!-- includeUsers members -->
					<#list command.members.includeUsers as _u>
						<input type="hidden" name="includeUsers" value="${_u}">
					</#list>

					<!-- includeUsers cmd -->
					<#list command.includeUsers as _u>
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
										class="btn disabled hide-checked-users has-tooltip"
										id="membership-remove-selected"
										<#if upstreamAssessmentGroups??>title="This will only adjust membership for this assignment in this app. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
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

														<#if item.itemType='exclude' && item.userId??><a class="btn btn-mini restore-user" data-usercode="${item.userId}">Restore</a></#if>

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

			var $assignmentGroupPicker = $('.ag-picker');

			var redoIndices = function(){
				$('.assessment-group-assignment').each(function(index){
					$(this).attr('name', 'assessmentGroupItems['+index+'].upstreamAssignment')
				});
				$('.assessment-group-occurrence').each(function(index){
					$(this).attr('name', 'assessmentGroupItems['+index+'].occurrence')
				});
			};

			// need to put this empty element in the form when all have been removed
			var emptyAssessmentGroups = '<input class="empty-group" type="hidden"  value="" name="assessmentGroups">';

			// remove linked groups
			$assignmentGroupPicker.on('click', 'tr.linked a', function(e){
				e.preventDefault();
				var $parentRow = $(this).closest("tr");
				var $parentCell = $(this).closest("td");
				var assignmentID = $(this).data("id");
				var occurrence = $(this).data("occurrence");
				$parentRow.removeClass("linked");
				// remove the linked group if one exists
				$('.linked-group[data-id='+assignmentID+'][data-occurrence='+occurrence+']').remove();
				// remove any pending new groups
				$('input', $parentRow).remove();
				// add the empty group element
				if($('.assessment-group-assignment,.linked-group').length == 0){
					$assignmentGroupPicker.append($(emptyAssessmentGroups));
				}
				redoIndices();
			});

			// add un-linked groups
			$assignmentGroupPicker.on('click', 'tr:not(.linked) a', function(e){
				e.preventDefault();

				// remove the empty group as one was added
				$('.empty-group').remove();

				var $parentRow = $(this).closest("tr");
				var $parentCell = $(this).closest("td");
				var index = $('.assessment-group-assignment').length
				var assignmentID = $(this).data("id");
				var occurrence = $(this).data("occurrence");
				var $assignmentElement = $('<input class="assessment-group-assignment" type="hidden" name="assessmentGroupItems['+index+'].upstreamAssignment" value="'+assignmentID+'"/>');
				var $occurrenceElement = $('<input class="assessment-group-occurrence" type="hidden" name="assessmentGroupItems['+index+'].occurrence" value="'+occurrence+'"/>');
				$parentRow.addClass("linked");
				$parentCell.append($assignmentElement);
				$parentCell.append($occurrenceElement);
			});

			var $membershipPicker = $('.membership-picker');
			var $form = $assignmentGroupPicker.closest('form');

			$assignmentGroupPicker.hide();
			$membershipPicker.hide();

			//close buttons on pickers
			$('.ag-picker, .membership-picker').find('.close').click(function(){
				var $close = $(this);
				$('.' + $close.data('dismiss')).hide();
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
			
			var $removeSelected = $('#membership-remove-selected');
			$membershipPicker.on('change', 'input.collection-checkbox', function() {
				$removeSelected.toggleClass('disabled', $membershipPicker.find('input.collection-checkbox:checked').length == 0);
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
						
			$('.restore-user').click(function(e) {
				e.preventDefault();
				var $this = $(this);
				$('#focusOn').val('member-list');
				var $usercode = $this.data('usercode');
				$membershipPicker.find('input:hidden[value='+ $usercode + '][name=excludeUsers]').remove();
				$this.closest('form').append(
					$('<input type=hidden name=includeUsers />').val($this.data('usercode'))
				);
				$(this).closest('tr').removeClass('item-type-exclude').addClass('item-type-sits');
				$(this).closest('tr').find('i.icon-minus-sign').remove();
				$(this).closest('tr').find('a.restore-user').remove();
			});
			
			$('.hide-checked-users').click(function(e) {
			    e.preventDefault();
				var checkedToRemove = $membershipPicker.find('input.collection-checkbox:checked')
				checkedToRemove.parents('.membership-item').hide();
				checkedToRemove.map(function() {
					$membershipPicker.find('input:hidden[value='+ this.value + '][name=includeUsers]').remove();
				});
			});

			$('select#academicYear').change(function(e) {
				refreshForm();
			});

			$('#show-sits-picker').click(function(){
				$('.membership-picker').hide();
				$('.ag-picker').toggle();
			});
			$('#show-membership-picker').click(function(){
				$('.ag-picker').hide();
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
