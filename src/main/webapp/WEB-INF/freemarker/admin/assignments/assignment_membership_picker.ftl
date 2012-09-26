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