<#--

	assignment membership editor split out from _fields.ftl for readability.

-->
<#escape x as x?html>
<@form.labelled_row "members" "Students">
		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>
		<#assign hasMembers=(membersGroup?? && (membersGroup.includeUsers?size gt 0 || membersGroup.excludeUsers?size gt 0)) />

		<#macro what_is_this>
			<#assign popoverText>
				<p>
					Here you can choose which students see this assignment.
			    	You can link to an assignment in SITS and the list of students will be updated automatically from there.
			    	If you are not using SITS you can manually add students by ITS usercode or university number.
				</p>
			     
				<p>
			    	It is also possible to tweak the list even when using SITS data, but this is only to be used
			    	when necessary and you still need to ensure that the upstream SITS data gets fixed.
		    	</p>
			</#assign>
		
			<a href="#"
			   title="What's this?"
			   class="use-popover" 
			   data-title="Student membership"
			   data-trigger="hover"
	   		   data-html="true"
			   data-content="${popoverText}"
			   ><i class="icon-question-sign"></i></a>
		</#macro>

		<#-- enumerate current state and offer change buttons -->
		<#if upstreamAssessmentGroups?has_content>
			<#assign sitsTotal=0 />
			<#list upstreamAssessmentGroups as group>
				<#assign sitsTotal = sitsTotal + group.members.members?size />
			</#list>
			<#if hasMembers>
				<#assign total = sitsTotal + membersGroup.includeUsers?size />
			<#else>
				<#assign total = sitsTotal />
			</#if>

			<span class="uneditable-value">
				${total} enrolled
				<#if hasMembers>
					(${sitsTotal} from SITS<#if membersGroup.excludeUsers?size gt 0>, after <@fmt.p membersGroup.excludeUsers?size "manual removal" /></#if><#if membersGroup.includeUsers?size gt 0>, plus <@fmt.p membersGroup.includeUsers?size "manual addition" /></#if>)
				</#if>
			<@what_is_this /></span>
			<div>
				<a class="btn" id="show-sits-picker">Change link to SITS</a> <a class="btn" id="show-membership-picker">Add/remove students manually</a>
			</div>
		<#elseif hasMembers>
			<span class="uneditable-value">${membersGroup.includeUsers?size} manually enrolled.
			<@what_is_this /></span>
			<div>
				<#if upstreamGroupOptions?has_content><a class="btn" id="show-sits-picker">Add link to SITS</a></#if> <a class="btn" id="show-membership-picker">Add/remove students manually</a>
			</div>
		<#else>
			<span class="uneditable-value">No students enrolled.</span>
			<@what_is_this /></span>
			<div>
				<#if upstreamGroupOptions?has_content><a class="btn" id="show-sits-picker">Add link to SITS</a></#if> <a class="btn" id="show-membership-picker">Add/remove students manually</a>
			</div>
		</#if>

		<div class="row-fluid">
			<div class="span10" style="min-height: 0;">
				<#-- Picker to select an upstream assessment group (upstreamassignment+occurrence) -->
				<div class="ag-picker hide">
					<a class="close" data-dismiss="ag-picker" aria-hidden="true">&times;</a>
					
					<#if command.upstreamGroupOptions?has_content>
						<h6>SITS link</h6>
						
						<p><small class="muted">Add students by linking this assignment to one or more of the following SITS assignments for
						${command.module.code?upper_case} which have assessment groups for ${command.academicYear.label}.</small></p>
						
						<table class="table table-bordered table-striped table-condensed table-hover table-sortable">
							<thead>
								<tr>
									<#-- FIXME: add jQ method to inject checkbox in first <th/>, and click handler to toggle all -->
									<th id="for-check-all"></th>
									<th class="sortable">Name</th>
									<th class="sortable">Members</th>
									<th class="sortable">CATS</th>
									<th class="sortable">Cohort</th>
									<th class="sortable">Sequence</th>
								</tr>
							</thead>
							<tbody><#list command.upstreamGroupOptions as option>
								<#assign isLinked = option.linked />
								<tr>
									<td><input type="checkbox" id="chk${option.assignmentId}${option.occurrence}" name="linked-upstream-assignment" value="${option.assignmentId},${option.occurrence}"></td>
									<td><label for="chk${option.assignmentId}${option.occurrence}">${option.name}<#if isLinked> <span class="label label-success">Linked</span></#if></label></td>
									<td>${option.memberCount}</td><#-- FIXME: <a/> popover (overflow-y: scroll) with member list -->
									<td>${option.cats!'-'}</td>
									<td>${option.occurrence}</td>
									<td>${option.sequence}</td>
								</tr>
							</#list></tbody>
						</table>

						<div class="picker-actions">
							<#-- FIXME: wireframe proposes .btn-success for add and .btn-danger for remove;
							with only one available depending on what's selected, and the other .disabled.
							Reality is that we can't predict what selection a user will make - it's entirely
							likely that they'll have a mix of already linked and not.
							Propose use uncontextualised actions of link and unlink,
							which are .disabled for null selection. -->
							<button class="btn btn-mini refresh-form disabled" id="add-sits-link">Link</button> <button class="btn btn-mini refresh-form disabled" id="remove-sits-link">Unlink</button>
						</div>

						<#-- FIXME: alerts fired post change go here, if controller returns something to say -->
						<p class="alert alert-success">This assignment is (now linked|no longer linked) to ${r"${name}"} and ${r"${name}"}</p>
					<#else>
						<p class="alert alert-warning">No SITS assignments for ${command.module.code?upper_case} are available</p>
					</#if>
				</div>

				<div class="membership-picker hide">
					<a class="close" data-dismiss="membership-picker" aria-hidden="true">&times;</a>
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
				</div><#-- .membership picker -->
			</div><#-- .span8 -->
		</div><#-- .row-fluid -->

		<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
		<script>
		jQuery(function($) {
			var $assignmentGroupPicker = $('.ag-picker');
			var $membershipPicker = $('.membership-picker');
			var $form = $assignmentGroupPicker.closest('form');

			<#-- sortable tables -->
			$('.table-sortable').sortableTable();
			
			<#-- enable picker close buttons -->
			$('.ag-picker, .membership-picker').find('.close').click(function() {
				var $close = $(this);
				$('.' + $close.data('dismiss')).hide();
			});
			
			<#-- controller detects action=refresh and does a bind without submit -->
			$('.refresh-form').on('click', '.btn:not.disabled', function(e) {
				e.preventDefault();
				$('#action-input').val('refresh');
				$form.submit();
			});
			
			<#-- clickable ag rows -->
			$('.ag-picker .table').on('click', 'tr', function(e) {
				if ($(e.target).is(':not(input:checkbox)')) {
					e.preventDefault();
					var chk = $(this).find('input:checkbox').get(0);
					chk.checked = !chk.checked;
				}
				
				var disable = $(this).siblings().andSelf().find('input:checked').length == 0;
				$('.ag-picker .picker-actions .btn').toggleClass('disabled', disable);
			});
			
			<#-- click handler --> 
			$('.ag-picker .picker-actions').on('click', '.btn', function(e) {
				e.preventDefault();
				if ($(this).is(':not(.disabled)')) {
					console.log('Clicked ' + $(this).text());
				}
			});

			<#-- refocus after click -->
			$('#add-members, #membership-remove-selected').click(function(e) {
				$('#focusOn').val('member-list');
			});

			<#-- button to unexclude excluded users -->
			$membershipPicker.find('.restore').click(function(e) {
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

			$('#show-sits-picker').click(function() {
				$('.membership-picker').hide();
				$('.ag-picker').toggle();
			});
			$('#show-membership-picker').click(function() {
				$('.ag-picker').hide();
				var $membershipPicker = $('.membership-picker');
				$membershipPicker.toggle();
				// switch straight to "Add more" tab if the group is empty
				if ($membershipPicker.is(":visible") && $membershipPicker.find('.tab-content table').length == 0) {
					$membershipPicker.find('a[href=#membership-tab2]').click();
				}
			});

			$('.has-tooltip').tooltip();

			<#assign focusOn=RequestParameters.focusOn!'' />
			<#if focusOn='member-list'>
				$('#show-membership-picker').click();
			</#if>
		});
		</script>

	</@form.labelled_row>
</#escape>