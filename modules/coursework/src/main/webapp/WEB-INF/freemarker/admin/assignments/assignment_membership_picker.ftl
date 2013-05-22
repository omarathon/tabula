<#--

	assignment membership editor split out from _fields.ftl for readability.

-->
<#escape x as x?html>
	<@form.labelled_row "members" "Students" "assignmentEnrolment">
		<@f.hidden path="upstreamAssignment" id="upstreamAssignment" />
		<@f.hidden path="occurrence" id="occurrence" />

		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>
		
		<#assign membershipDetails = command.membershipDetails />
		<#assign hasMembers = (membershipDetails?size gt 0) />
		
		<#macro what_is_this>
			<#assign popoverText>
				<p>Expand to view and choose which students see this assignment.</p>
				
				<p>You can link to an assignment in SITS and the list of students will be updated automatically from there.
				If you are not using SITS you can manually add students by ITS usercode or university number.</p>
				
				<p>It is also possible to tweak the list even when using SITS data, but this is only to be used
				when necessary and you still need to ensure that the upstream SITS data gets fixed.</p>
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
		
		<details>
			<summary>
				<#-- enumerate current state -->
				<#if upstreamAssessmentGroups?has_content>
					<#assign sitsTotal = 0 />
					<#list upstreamAssessmentGroups as group>
						<#assign sitsTotal = sitsTotal + group.members.members?size />
					</#list>
					<#assign sitsTotal = sitsTotal - (membersGroup.excludeUsers?size)!0 />
					<#assign total = sitsTotal + (membersGroup.includeUsers?size)!0 />
					
					<span class="uneditable-value">
						${total} enrolled
						<#if hasMembers>
							(${sitsTotal} <#if membersGroup.excludeUsers?size gt 0>left</#if> from SITS<#if membersGroup.excludeUsers?size gt 0> after <@fmt.p membersGroup.excludeUsers?size "manual removal" /></#if><#if membersGroup.includeUsers?size gt 0>, plus <@fmt.p membersGroup.includeUsers?size "manual addition" /></#if>)
						<#else>
							from SITS
						</#if>
					<@what_is_this /></span>
				<#elseif hasMembers>
					<span class="uneditable-value">${membersGroup.includeUsers?size} manually enrolled.
					<@what_is_this /></span>
				<#else>
					<span class="uneditable-value">No students enrolled.
					<@what_is_this /></span>
				</#if>
			</summary>

			<#-- FIXME: alerts fired post SITS change go here, if controller returns something to say -->
			<#-- <p class="alert alert-success"><i class="icon-ok"></i> This assignment is (now linked|no longer linked) to ${r"${name}"} and ${r"${name}"}</p> -->
		
			<p>
				<#if upstreamAssessmentGroups??>
					<a class="btn use-tooltip" id="show-sits-picker" title="Change the linked SITS assignment used for enrolment data">Change link to SITS</a>
				<#elseif upstreamGroupOptions??>
					<a class="btn use-tooltip" id="show-sits-picker" title="Use enrolment data from one or more assignments recorded in SITS">Add link to SITS</a>
				<#else>
					<a class="btn use-tooltip disabled" title="No assignments are recorded for this module in SITS. Add them there if you want to create a parallel link in Tabula.">No SITS link available</a>
				</#if>
				</a>
			
				<a class="btn use-tooltip" id="show-adder"
						<#if upstreamAssessmentGroups??>title="This will only enrol a student for this assignment in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
						>
					Add students manually
				</a>
				
				<a class="btn btn-warning disabled remove-users member-action use-tooltip"
						<#if upstreamAssessmentGroups??>title="This will only remove enrolment for this assignment in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
						>
					Remove
				</a> 
				
				<a class="btn restore-users disabled use-tooltip" title="Re-enrol selected students">Restore</a>
			</p>

			<#if hasMembers>
				<#assign includeIcon><span class="use-tooltip" title="Added manually" data-placement="right"><i class="icon-hand-up"></i></span><span class="hide">Added</span></#assign>
				<#assign excludeIcon><span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="icon-remove"></i></span><span class="hide">Removed</span></#assign>
				<#assign sitsIcon><span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="icon-list-alt"></i></span><span class="hide">SITS</span></#assign>
				
				<div id="enrolment" class="scroller">
					<table id="enrolment-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
						<thead>
							<tr>
								<th class="for-check-all" style="width: 25px;"></th>
								<th class="sortable" style="width: 60px;">Source</th>
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
										<#if item.userId??>
											<@form.selector_check_row "modifyEnrolment" item.userId />
										<#else>
											<i class="icon-ban-circle use-tooltip" title="We are missing this person's usercode, without which we cannot modify their enrolment."></i>
										</#if>
									</td>
									<td class="source">
										<#noescape>
											<#if item.itemType='include'>
												${includeIcon}
											<#elseif item.itemType='exclude'>
												${excludeIcon}
											<#else>
												${sitsIcon}
											</#if>
										</#noescape>
									</td>
									<td>
										<#if _u.foundUser>
											${_u.firstName}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
									</td>
									<td>
										<#if _u.foundUser>
											${_u.lastName}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
									</td>
									<td>
										<#if item.universityId??>
											${item.universityId}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
									</td>
									<td>
										<#if _u.foundUser>
											${_u.userId}
										<#elseif item.userId??>
											${item.userId}
										<#else><#-- Hmm this bit shouldn't ever happen -->
											<span class="muted">Unknown</span>
										</#if>
									</td>
								</tr>
							</#list>
						</tbody>
						
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
				<p class="muted">
					Type or paste in a list of usercodes or University numbers here, separated by white space, then click <code>Add</code>.
				</p>
				<p class="muted">
					<strong>Is your module in SITS?</strong> It may be better to fix the data there,
					as other University systems won't know about any changes you make here.
				</p>
				<#-- SOON
				<div>
					<a href="#" class="btn"><i class="icon-user"></i> Lookup user</a>
				</div>
				-->
				<textarea rows="6" class="input-block-level" name="massAddUsers"></textarea>
			</div>
				
			<div class="modal-footer">
				<a class="btn btn-success refresh-form disabled" id="add-students">Add</a>
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
					
					<table id="sits-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
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
			
			var $enrolmentTable = $('#enrolment-table');
			var $pendingAlert = $('<p class="alert alert-warning hide"><i class="icon-warning-sign"></i> Your changes will not be recorded until you save this assignment.</p>');
			
			<#-- FIXME: temporary pop-out hiding. Do this properly at source in SBTWO idscripts -->
			setTimeout(function() { $('.sb-table-wrapper-popout').remove() }, 500);
			
			<#-- controller detects action=refresh and does a bind without submit -->
			<#--
			 $('.refresh-form').on('click', '.btn:not.disabled', function(e) {
				e.preventDefault();
				$('#action-input').val('refresh');
				$(this).closest('form').submit();
			});
			-->
			
			<#-- manage check-all state -->
			var updateCheckboxes = function($table) {
				var checked = $table.find('td input:checked').length;
				var disable = (checked == 0);
				if (checked == $table.find('td input').length) $table.find('.check-all').prop('checked', true);
				if (checked == 0) $table.find('.check-all').prop('checked', false);
			}
			
			<#-- en/disable action buttons -->
			var enableActions = function($table) {
				var context = $table.prop('id');
				
				if (context == 'sits-table') {
					$('.sits-picker-action').toggleClass('disabled', $table.find('input:checked').length==0);
				} else if (context == 'enrolment-table') {
					$('.remove-users').toggleClass('disabled', $table.find('tr.item-type-include input:checked, tr.sits input:checked').length==0);
					$('.restore-users').toggleClass('disabled', $table.find('tr.item-type-exclude input:checked').length==0);
				}
			}
			
			<#-- make table rows clickable -->
			$('.table-checkable').on('click', 'tr', function(e) {
				if ($(e.target).is(':not(input:checkbox)')) {
					e.preventDefault();
					var $chk = $(this).find('input:checkbox');
					if ($chk.length) {
						$chk.prop('checked', !$chk.prop('checked'));
					}
				}
				
				var $table = $(this).closest('table');
				updateCheckboxes($table);
				enableActions($table);
			});
			
			<#-- dynamically attach check-all checkbox -->
			$('.for-check-all').append($('<input />', { type: 'checkbox', class: 'check-all use-tooltip', title: 'Select all/none' }));
			$('.check-all').tooltip({ delay: 1500 });
			$('table th').on('click', '.check-all', function(e) {
				var $table = $(this).closest('table');
				var checkStatus = this.checked;
				$table.find('td input:checkbox').prop('checked', checkStatus);
				
				updateCheckboxes($table);
				enableActions($table);
			});
			
			<#-- sits-picker click handler -->
			$('#sits-picker').on('click', '.btn', function(e) {
				e.preventDefault();
				if ($(this).is(':not(.disabled)')) {
					console.log('Clicked ' + $(this).text());
					<#-- FIXME: update list of students in main view.
					
					Do an AJAX call to get the list of students from SITS in selected groups,
					mixin manual edits and return as JSON for injection into the master list -->
					$('#focusOn').val('#enrolment-table');
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
					$('#focusOn').val('#enrolment-table');
				}
			});

			<#-- adder dis/enabled -->
			$('#adder').on('input propertychange', 'textarea', function(e) {
				e.preventDefault();
				var empty = ($.trim($(this).val()) == "");
				$('#add-students').toggleClass('disabled', empty);
			});
			
			<#-- show modals -->
			$('#show-sits-picker').click(function() {
				$('#sits-picker').modal('show');
			});
			$('#show-adder').click(function() {
				$('#adder').modal('show');
			});
			
			<#-- remove user from enrolment table -->
			$('.remove-users').click(function(e) {
				e.preventDefault();
				$enrolmentTable.find('tr.item-type-include input:checked, tr.item-type-sits input:checked').each(function() {
					var usercode = $(this).val();
					var $tr = $(this).closest('tr');
					
					// update the hidden fields 
					$enrolmentTable.find('input:hidden[name=includeUsers][value='+ usercode + ']').remove();
					$enrolmentTable.append($('<input type=hidden name=excludeUsers />').val(usercode));
					
					// update rendering
					$tr.find('.source').html('<#noescape>${excludeIcon}</#noescape>').append($(' <span class="label">Pending</span>'));
					$tr.removeClass(function(i, css) {
						return (css.match(/\bitem-type-\S+/g) || []).join(' ');
					}).addClass('item-type-exclude');
					
					this.checked = '';
					
					$('#enrolment').before($pendingAlert);
					$pendingAlert.slideDown();
				});
			});
			
			<#-- restore excluded user -->
			$('.restore-users').click(function(e) {
				e.preventDefault();
				$enrolmentTable.find('tr.item-type-exclude input:checked').each(function() {
					var usercode = $(this).val();
					var $tr = $(this).closest('tr');
					
					// update the hidden fields 
					$enrolmentTable.find('input:hidden[name=excludeUsers][value='+ usercode + ']').remove();
					$enrolmentTable.append($('<input type=hidden name=includeUsers />').val(usercode));
					
					// update rendering
					$tr.find('.source').html('<#noescape>${includeIcon}</#noescape>').append($(' <span class="label">Pending</span>'));
					$tr.removeClass(function(i, css) {
						return (css.match(/\bitem-type-\S+/g) || []).join(' ');
					}).addClass('item-type-include');
					
					this.checked = '';
					
					$('#enrolment').before($pendingAlert);
					$pendingAlert.slideDown();
				});
			});
			
		
		
			
			
			
			$('.hide-checked-users').click(function(e) {
			    e.preventDefault();
				var checkedToRemove = $enrolmentTable.find('input.collection-checkbox:checked')
				checkedToRemove.parents('.membership-item').hide();
				checkedToRemove.map(function() {
					$enrolmentTable.find('input:hidden[value='+ this.value + '][name=includeUsers]').remove();
				});
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