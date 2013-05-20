<#--

	assignment membership editor split out from _fields.ftl for readability.

-->
<#escape x as x?html>
<@form.labelled_row "members" "Students">
		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>
		
		<#assign membershipDetails = command.membershipDetails />
		<#assign hasMembers = (membershipDetails?size gt 0) />
		
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

		<details class="studentManagement">
			<summary>
				<#-- enumerate current state and offer change buttons -->
				<#if upstreamAssessmentGroups?has_content>
					<#assign sitsTotal = 0 />
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
							(${sitsTotal} <#if membersGroup.excludeUsers?size gt 0>left</#if> from SITS<#if membersGroup.excludeUsers?size gt 0> after <@fmt.p membersGroup.excludeUsers?size "manual removal" /></#if><#if membersGroup.includeUsers?size gt 0>, plus <@fmt.p membersGroup.includeUsers?size "manual addition" /></#if>)
						</#if>
					<@what_is_this /></span>
				<#elseif hasMembers>
					<span class="uneditable-value">${membersGroup.includeUsers?size} manually enrolled.
					<@what_is_this /></span>
				<#else>
					<span class="uneditable-value">No students enrolled.</span>
					<@what_is_this /></span>
				</#if>
			</summary>

			<#-- FIXME: alerts fired post SITS change go here, if controller returns something to say -->
			<p class="alert alert-success"><i class="icon-ok"></i> This assignment is (now linked|no longer linked) to ${r"${name}"} and ${r"${name}"}</p>
		
			<div style="margin-bottom: 12px;">
				<#if sitsTotal??>
					<a class="btn" id="show-sits-picker">
						<#if upstreamAssessmentGroups?has_content>
							Change link to SITS
						<#else>
							Add link to SITS
						</#if>
					</a>
				</#if>
				
				<a class="btn" id="show-adder">Add students manually</a>
				
				<a class="btn btn-warning disabled hide-checked-users member-action use-tooltip"
						id="membership-remove-selected"
						<#if upstreamAssessmentGroups??>title="This will only adjust membership for this assignment in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
						>
					Remove selected
				</a>
			</div>

			<#-- includeUsers members -->
			<#list command.members.includeUsers as _u>
				<input type="hidden" name="includeUsers" value="${_u}">
			</#list>

			<#-- existing includeUsers cmd -->
			<#list command.includeUsers as _u>
				<input type="hidden" name="includeUsers" value="${_u}">
			</#list>

			<#list command.members.excludeUsers as _u>
				<input type="hidden" name="excludeUsers" value="${_u}">
			</#list>

			<#if hasMembers>
				<div id="member-list" class="scroller">
					<table class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers" data-controller=".member-action">
						<thead>
							<tr>
								<th class="for-check-all" style="width: 45px;"></th>
								<th class="sortable" style="width: 45px;">Source</th>
								<th class="sortable">First name</th>
								<th class="sortable">Last name</th>
								<th class="sortable">ID</th>
								<th class="sortable">User</th>
							</tr>
						</thead>
						
						<tbody>
							<#list membershipDetails as item>
								<#assign _u = item.user>
								
								<tr class="membership-item item-type-${item.itemType}"> <#-- item-type-(sits|include|exclude) -->
									<td>
										<#if item.itemType != 'exclude'>
											<#--
												TODO checkboxes are currently all named "excludeUsers", relying on the fact that we only
												use the checkboxes for removing users. If we add other options then this will need changing
												and probably the script will need to generate hidden inputs instead. As it is, the checkboxes
												generate the formdata that we want and so we can just submit it.
											-->
											<@form.selector_check_row "excludeUsers" item.userId />
										<#elseif item.userId??>
											<a class="btn btn-mini btn-success restore-user use-tooltip" data-usercode="${item.userId}" data-placement="left" title="Reinstate normal SITS enrolment for this student">Restore</a>
										</#if>
									</td>
									<td>
										<#if item.itemType='include'>
											<span class="use-tooltip" title="Added manually" data-placement="right"><i class="icon-hand-up"></i></span><span class="hide">Added</span>
										<#elseif item.itemType='exclude'>
											<span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="icon-remove"></i></span><span class="hide">Removed</span>
										<#else>
											<span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="icon-list-alt"></i></span><span class="hide">SITS</span>
										</#if>
									</td>
									<td><#if item.itemType='exclude'><del></#if>
										<#if _u.foundUser>
											${_u.firstName}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
										<#if item.itemType='exclude'></del></#if>
									</td>
									<td><#if item.itemType='exclude'><del></#if>
										<#if _u.foundUser>
											${_u.lastName}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
										<#if item.itemType='exclude'></del></#if>
									</td>
									<td><#if item.itemType='exclude'><del></#if>
										<#if item.universityId??>
											${item.universityId}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
										<#if item.itemType='exclude'></del></#if>
									</td>
									<td><#if item.itemType='exclude'><del></#if>
										<#if _u.foundUser>
											${_u.userId}
										<#elseif item.userId??>
											${item.userId}
										<#else><#-- Hmm this bit shouldn't ever happen -->
											<span class="muted">Unknown</span>
										</#if>
										<#if item.itemType='exclude'></del></#if>
									</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</div>
			<#else>
				<p class="alert alert-warning">No students yet.</p>
			</#if>
		</details>
		
		<#-- Modal to add students manually -->
		<div id="adder" class="modal fade hide">
			<div class="modal-header">
				<a class="close" data-dismiss="modal" aria-hidden="true">&times;</a>
				<h6>Add students manually</h6>
			</div>
			
			<div class="modal-body">
				<p>
					Type or paste in a list of usercodes or University numbers here then click Add.
					<br>
					<strong>Is your module in SITS?</strong> It may be better to fix the data there,
					as other University systems won't know about any changes you make here.
				</p>
				<#-- SOON
				<div>
					<a href="#" class="btn"><i class="icon-user"></i> Lookup user</a>
				</div>
				-->
				<textarea class="input-block-level" name="massAddUsers"></textarea>
			</div>
				
			<div class="modal-footer">
				<a class="btn btn-success refresh-form disabled" id="add-students">Add</a>
				<a class="btn cancel">Cancel</a>
			</div>
		</div>
		
		
		<#-- Modal picker to select an upstream assessment group (upstreamassignment+occurrence) -->
		<div id="sits-picker" class="modal fade hide">
			<div class="modal-header">
				<a class="close" data-dismiss="modal" aria-hidden="true">&times;</a>
				<h6>SITS link</h6>
			</div>
			
			<#if command.upstreamGroupOptions?has_content>
				<div class="modal-body">
					<p class="muted">Add students by linking this assignment to one or more of the following SITS assignments for
					${command.module.code?upper_case} which have assessment groups for ${command.academicYear.label}.</p>
					
					<table class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers" data-controller=".sits-picker-action">
						<thead>
							<tr>
								<th class="for-check-all"></th>
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
				</div>
				
				<div class="modal-footer">
					<a class="btn btn-success refresh-form disabled sits-picker-action" id="add-sits-link">Link</a>
					<a class="btn btn-warning refresh-form disabled sits-picker-action" id="remove-sits-link">Unlink</a>
					<a class="btn cancel">Cancel</a>
				</div>
			<#else>
				<div class="modal-body">
					<p class="alert alert-warning">No SITS assignments for ${command.module.code?upper_case} are available</p>
				</div>
			</#if>
		</div>

		<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
		<script>
		jQuery(function($) {
			<#-- sortable tables -->
			$('.table-sortable').sortableTable();
			
			<#-- FIXME: temporary pop-out hiding. Do this properly at source in SBTWO idscripts -->
			setTimeout(function() { $('.sb-table-wrapper-popout').remove() }, 500);
			
			<#-- controller detects action=refresh and does a bind without submit -->
			$('.refresh-form').on('click', '.btn:not.disabled', function(e) {
				e.preventDefault();
				$('#action-input').val('refresh');
				$(this).closest('form').submit();
			});
			
			<#-- manage check-all state and en/disable buttons -->
			var updateTable = function($table) {
				var checked = $table.find('td input:checked').length;
				var disable = (checked == 0);
				if (checked == $table.find('td input').length) $table.find('.check-all').prop('checked', true);
				if (checked == 0) $table.find('.check-all').prop('checked', false);
				
				var $controller = $($table.data('controller'));
				$controller.toggleClass('disabled', (checked == 0));
			}
			
			<#-- make modal rows clickable -->
			$('.table-checkable').on('click', 'tr', function(e) {
				if ($(e.target).is(':not(input:checkbox)')) {
					e.preventDefault();
					var $chk = $(this).find('input:checkbox');
					if ($chk.length) {
						$chk.prop('checked', !$chk.prop('checked'));
					}
				}
				
				updateTable($(this).closest('table'));
			});
			
			<#-- dynamically attach check-all checkbox -->
			$('.for-check-all').append($('<input />', { type: 'checkbox', class: 'check-all use-tooltip', title: 'Select all/none' }));
			$('.check-all').tooltip({ delay: 1500 });
			$('table th').on('click', '.check-all', function(e) {
				var checkStatus = this.checked;
				$(this).closest('table').find('td input:checkbox').prop('checked', checkStatus);
				
				updateTable($(this).closest('table'));
			});
			
			<#-- sits-picker click handler -->
			$('#sits-picker').on('click', '.btn', function(e) {
				e.preventDefault();
				if ($(this).is(':not(.disabled)')) {
					console.log('Clicked ' + $(this).text());
					<#-- FIXME: update list of students in main view.
					
					Do an AJAX call to get the list of students from SITS in selected groups,
					mixin manual edits and return as JSON for injection into the master list -->
				}
			});

			<#-- adder click handler -->
			$('#adder').on('click', '.btn', function(e) {
				e.preventDefault();
				if ($(this).is(':not(.disabled)')) {
					console.log('Clicked ' + $(this).text());
					<#-- FIXME: add students to list in main view.
					
					Do an AJAX call to get the list of students from SITS in selected groups,
					mixin manual edits (including new data) and return as JSON for injection into the master list -->
				}
			});

			<#-- adder dis/enabled -->
			$('#adder').on('input propertychange', 'textarea', function(e) {
				e.preventDefault();
				var empty = ($.trim($(this).val()) == "");
				$('#add-students').toggleClass('disabled', empty);
			});
			
			<#-- cancel button -->
			$('.modal').on('click', '.cancel', function(e) {
				$(this).closest('.modal').modal('hide');
			});

			<#-- refocus after click -->
			$('#add-members, #membership-remove-selected').click(function(e) {
				$('#focusOn').val('member-list');
			});
			
			var $membershipPicker = $('#member-list');

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
				$('#sits-picker').modal('show');
			});
			$('#show-adder').click(function() {
				$('#adder').modal('show');
			});

			<#assign focusOn=RequestParameters.focusOn!'' />
			<#if focusOn='member-list'>
				<#-- FIXME scrollto-->
				$("html, body").animate({
					scrollTop: $('#member-list').offset().top - window.getNavigationHeight()
				}, 300);				
			</#if>
		});
		</script>

	</@form.labelled_row>
</#escape>