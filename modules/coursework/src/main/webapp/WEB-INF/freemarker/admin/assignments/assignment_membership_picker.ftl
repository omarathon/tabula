<#--

	assignment membership editor split out from _fields.ftl for readability.

-->
<#escape x as x?html>
	<@form.labelled_row "members" "Students" "assignmentEnrolment">
		<fieldset id="assignmentEnrolmentFields">

		<#list command.upstreamGroups as item>
			<@f.hidden path="upstreamGroups[${item_index}]" cssClass="upstreamGroups" />
		</#list>

		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>

		<#assign includeIcon><span class="use-tooltip" title="Added manually" data-placement="right"><i class="icon-hand-up"></i></span><span class="hide">Added</span></#assign>
		<#assign pendingDeletionIcon><span class="use-tooltip" title="Deleted manual addition" data-placement="right"><i class="icon-remove"></i></span><span class="hide">Pending deletion</span></#assign>
		<#assign excludeIcon><span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="icon-ban-circle"></i></span><span class="hide">Removed</span></#assign>
		<#assign sitsIcon><span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="icon-list-alt"></i></span><span class="hide">SITS</span></#assign>

		<#assign assignmentMembership = command.assignmentMembership />
		<#assign hasMembers = assignmentMembership.totalCount gt 0 />

		<#macro what_is_this>
			<#assign popoverText>
				<p>You can link to an assignment in SITS and the list of students will be updated automatically from there.
				If you are not using SITS you can manually add students by ITS usercode or university number.</p>

				<p>It is also possible to tweak the list even when using SITS data, but this is only to be used
				when necessary and you still need to ensure that the upstream SITS data gets fixed.</p>
			</#assign>

			<a href="#"
			   title="What's this?"
			   class="use-popover"
			   data-title="Students"
			   data-trigger="hover"
	   		   data-html="true"
			   data-content="${popoverText}"
			   ><i class="icon-question-sign"></i></a>
		</#macro>

		<details>
			<summary>
				<#-- enumerate current state -->
				<#if linkedUpstreamAssessmentGroups?has_content>
					<span class="uneditable-value">
						${assignmentMembership.totalCount} enrolled
						<#if assignmentMembership.excludeCount gt 0 || assignmentMembership.includeCount gt 0>
							<span class="muted">(${assignmentMembership.sitsCount} from SITS<#if assignmentMembership.usedExcludeCount gt 0> after ${assignmentMembership.usedExcludeCount} removed manually</#if><#if assignmentMembership.usedIncludeCount gt 0>, plus ${assignmentMembership.usedIncludeCount} added manually</#if>)</span>
						<#else>
							<span class="muted">from SITS</span>
						</#if>
					<@what_is_this /></span>
				<#elseif hasMembers>
					<span class="uneditable-value">${assignmentMembership.includeCount} manually enrolled
					<@what_is_this /></span>
				<#else>
					<span class="uneditable-value">No students enrolled
					<@what_is_this /></span>
				</#if>
			</summary>

			<#-- FIXME: alerts fired post SITS change go here, if controller returns something to say -->
			<#-- <p class="alert alert-success"><i class="icon-ok"></i> This assignment is (now linked|no longer linked) to ${r"${name}"} and ${r"${name}"}</p> -->

			<p>
				<#if linkedUpstreamAssessmentGroups?has_content>
					<a class="btn use-tooltip disabled" id="show-sits-picker" title="Change the linked SITS assignment used for enrolment data">Change link to SITS</a>
				<#elseif availableUpstreamGroups?has_content>
					<a class="btn use-tooltip disabled" id="show-sits-picker" title="Use enrolment data from one or more assignments recorded in SITS">Add link to SITS</a>
				<#else>
					<a class="btn use-tooltip disabled" title="No assignments are recorded for this module in SITS. Add them there if you want to create a parallel link in Tabula.">No SITS link available</a>
				</#if>
				</a>

				<a class="btn use-tooltip disabled" id="show-adder"
						<#if availableUpstreamGroups??>title="This will only enrol a student for this assignment in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
						>
					Add students manually
				</a>

				<#if hasMembers>
					<a class="btn btn-warning disabled remove-users member-action use-tooltip"
							<#if availableUpstreamGroups??>title="This will only remove enrolment for this assignment in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
							>
						Remove
					</a>

					<a class="btn btn-success restore-users disabled use-tooltip" title="Re-enrol selected students">Restore</a>
				</#if>

				<span class="help-inline" id="js-hint"><small><i class="icon-lightbulb"></i> Javascript is required for editing</small></span>
			</p>

			<#if hasMembers>
				<div id="enrolment">
					<table id="enrolment-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers tabula-orangeLight">
						<thead>
							<tr>
								<th class="for-check-all" style="width: 20px; padding-right: 0;"></th>
								<th class="sortable" style="width: 50px;">Source</th>
								<th class="sortable">First name</th>
								<th class="sortable">Last name</th>
								<th class="sortable">ID</th>
								<th class="sortable">User</th>
							</tr>
						</thead>

						<tbody>
							<#list assignmentMembership.items as item>
								<#assign _u = item.user>

								<tr class="membership-item item-type-${item.itemTypeString}"> <#-- item-type-(sits|include|exclude) -->
									<td>
										<#if item.userId??>
											<@form.selector_check_row "modifyEnrolment" item.userId />
										<#else>
											<i class="icon-ban-circle use-tooltip" title="We are missing this person's usercode, without which we cannot modify their enrolment."></i>
										</#if>
									</td>
									<td class="source">
										<#noescape>
											<#if item.itemTypeString='include'>
												${includeIcon}
											<#elseif item.itemTypeString='exclude'>
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

						<#list command.members.includeUsers as usercode>
							<input type="hidden" name="includeUsers" value="${usercode}">
						</#list>

						<#list command.members.excludeUsers as usercode>
							<input type="hidden" name="excludeUsers" value="${usercode}">
						</#list>
					</table>
				</div>
			<#else>
				<#-- placeholder to allow new links to be appended via script -->
				<span id="enrolment-table"></span>
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
					Type or paste in a list of usercodes or University numbers here, separated by white space, then click <code>Add</code>.
				</p>
				<p class="alert">
					<i class="icon-lightbulb icon-large"></i> <strong>Is your module in SITS?</strong> It may be better to fix the data there,
					as other University systems won't know about any changes you make here.
				</p>
				<#-- SOMETIME
				<div>
					<a href="#" class="btn"><i class="icon-user"></i> Lookup user</a>
				</div>
				-->
				<textarea rows="6" class="input-block-level" name="massAddUsers"></textarea>
			</div>

			<div class="modal-footer">
				<a class="btn btn-success disabled spinnable spinner-auto" id="add-students">Add</a>
			</div>
		</div>


		<#-- Modal picker to select an upstream assessment group (upstreamassignment+occurrence) -->
		<div id="sits-picker" class="modal fade hide">
			<div class="modal-header">
				<a class="close" data-dismiss="modal" aria-hidden="true">&times;</a>
				<h6>SITS link</h6>
			</div>

			<#if command.availableUpstreamGroups?has_content>
				<div class="modal-body">
					<p>Add students by linking this assignment to one or more of the following SITS assignments for
					${command.module.code?upper_case} which have assessment groups for ${command.academicYear.label}.</p>

					<table id="sits-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers tabula-orangeLight">
						<thead>
							<tr>
								<th class="for-check-all" style="width: 20px; padding-right: 0;"></th>
								<th class="sortable">Name</th>
								<th class="sortable">Members</th>
								<th class="sortable">CATS</th>
								<th class="sortable">Cohort</th>
								<th class="sortable">Sequence</th>
							</tr>
						</thead>
						<tbody><#list command.availableUpstreamGroups as available>
							<#assign isLinked = available.isLinked(command.assessmentGroups) />
							<tr>
								<td><input type="checkbox" id="chk-${available.id}" name="" value="${available.id}"></td>
								<td><label for="chk-${available.id}">${available.name}<#if isLinked> <span class="label label-success">Linked</span></#if></label></td>
								<td>${available.memberCount}</td><#-- FIXME: <a/> popover (overflow-y: scroll) with member list -->
								<td>${available.cats!'-'}</td>
								<td>${available.occurrence}</td>
								<td>${available.sequence}</td>
							</tr>
						</#list></tbody>
					</table>
				</div>

				<div class="modal-footer">
					<a class="btn btn-success disabled sits-picker-action spinnable spinner-auto" id="link-sits">Link</a>
					<a class="btn btn-warning disabled sits-picker-action spinnable spinner-auto" id="unlink-sits">Unlink</a>
				</div>
			<#else>
				<div class="modal-body">
					<p class="alert alert-warning">No SITS assignments for ${command.module.code?upper_case} are available</p>
				</div>
			</#if>
		</div>
		</fieldset>
	</@form.labelled_row>

	<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
	<script>
	jQuery(function($) {
		var $enrolment = $('.assignmentEnrolment');

		var initEnrolment = function() {
			<#-- well, if we're here, JS must be available :) -->
			$('#js-hint').remove();
			$('#show-sits-picker, #show-adder').removeClass('disabled');

			<#-- sortable tables -->
			$enrolment.find('.table-sortable').sortableTable();
			$enrolment.tabulaPrepareSpinners();
			$enrolment.find('details').details();

			<#-- FIXME: temporary pop-out hiding. Do this properly at source in SBTWO idscripts -->
			setTimeout(function() { $('.sb-table-wrapper-popout').remove() }, 500);

			<#-- dynamically attach check-all checkbox -->
			$('.for-check-all').append($('<input />', { type: 'checkbox', class: 'check-all use-tooltip', title: 'Select all/none' }));
			$('.check-all').tooltip({ delay: 1500 });
			$enrolment.on('click', '.table-checkable th .check-all', function(e) {
				var $table = $(this).closest('table');
				var checkStatus = this.checked;
				$table.find('td input:checkbox').prop('checked', checkStatus);

				updateCheckboxes($table);
				enableActions($table);
			});

			<#-- preset to open -->
			if ($enrolment.data('open')) {
				$('.assignmentEnrolment details').prop('open', 'open');
				$("html, body").delay(200).animate({
					scrollTop: $enrolment.offset().top - window.id6nav.navigationHeight
				}, 300);
			}
		}

		<#-- initialise the scripting for enrolment management -->
		<#if RequestParameters.open?? || openDetails!false>
			$enrolment.data('open', true);
		</#if>
		initEnrolment();

		var $pendingAlert = $('<p class="alert alert-warning hide"><i class="icon-warning-sign"></i> Your changes will not be recorded until you save this assignment.	<input type="submit" value="Save" class="btn btn-primary btn-mini update-only"></p>');

		<#-- manage check-all state -->
		var updateCheckboxes = function($table) {
			var checked = $table.find('td input:checked').length;
			if (checked == $table.find('td input').length) $table.find('.check-all').prop('checked', true);
			if (checked == 0) $table.find('.check-all').prop('checked', false);
		}

		<#-- en/disable action buttons -->
		var enableActions = function($table) {
			var context = $table.prop('id');

			if (context == 'sits-table') {
				$('.sits-picker-action').toggleClass('disabled', $table.find('input:checked').length==0);
			} else if (context == 'enrolment-table') {
				$('.remove-users').toggleClass('disabled', $table.find('tr.item-type-include input:checked, tr.item-type-sits input:checked').length==0);
				$('.restore-users').toggleClass('disabled', $table.find('tr.item-type-exclude input:checked').length==0);
			}
		}

		var alertPending = function() {
			if (window.location.pathname.indexOf('/assignments/new') == -1) {
				$('#enrolment').before($pendingAlert);
				$pendingAlert.delay(750).slideDown();
			}
		}

		<#-- make table rows clickable -->
		$enrolment.on('click', '.table-checkable tr', function(e) {
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

		<#-- sits-picker click handler -->
		$enrolment.on('click', '#sits-picker .btn', function(e) {
			e.preventDefault();
			var $m = $(this).closest('.modal');
			if ($(this).is(':not(.disabled)')) {
				$('#sits-picker .btn').addClass('disabled').prop('disabled', 'disabled');

				<#-- get current list of values and remove and/or add changes -->
				var current = $('.upstreamGroups').map(function(i, input) { return input.value }).toArray();
				var changes = $m.find('td input:checked').map(function(i, input) { return input.value }).toArray();
				// always remove even when adding, to dedupe
				var data = $(current).not(changes).toArray();
				if (this.id == 'link-sits') {
					data = data.concat(changes);
				}

				var $newInputs = $(data).map(function(i, value) {
					return $('<input>', { class: 'upstreamGroups', type: 'hidden', name: 'upstreamGroups['+i+']', value:value })[0];
				});
				$('.upstreamGroups').remove();
				$('#enrolment-table').append($newInputs);

				$.ajax({
					type: 'POST',
					url: '<@routes.assignmentenrolment module />',
					data: $('#assignmentEnrolmentFields').serialize(),
					error: function() {
						$m.modal('hide');
					},
					success: function(data, status) {
						$m.modal('hide');
						$enrolment.html($(data).find('.assignmentEnrolment').contents());
						$enrolment.data('open', true);
						initEnrolment();
						alertPending();
					}
				});
			}
		});

		<#-- adder click handler -->
		$enrolment.on('click', '#adder .btn', function(e) {
			e.preventDefault();
			var $m = $(this).closest('.modal');
			if ($(this).is(':not(.disabled)')) {
				$(this).addClass('disabled').prop('disabled', 'disabled');
				$.ajax({
					type: 'POST',
					url: '<@routes.assignmentenrolment module />',
					data: $('#assignmentEnrolmentFields').serialize(),
					error: function() {
						$m.modal('hide');
					},
					success: function(data, status) {
						$m.modal('hide');
						$enrolment.html($(data).find('.assignmentEnrolment').contents());
						$enrolment.data('open', true);
						initEnrolment();
						alertPending();
					}
				});
			}
		});

		<#-- adder dis/enabled -->
		$enrolment.on('input propertychange', '#adder textarea', function(e) {
			e.preventDefault();
			var empty = ($.trim($(this).val()) == "");
			$('#add-students').toggleClass('disabled', empty);
		});

		<#-- show modals -->
		$enrolment.on('click', '#show-sits-picker', function() {
			$('#sits-picker').modal('show');
		});
		$enrolment.on('click', '#show-adder', function() {
			$('#adder').on('shown', function() {
				$(this).find('textarea').focus();
			}).modal('show');
		});

		<#-- reset on modal close -->
		$enrolment.on('hidden', '.modal', function(e) {
			$(this).find('input:checked').removeAttr('checked');
			$(this).find('.spinnable').spin(false);
		});

		<#-- remove user from enrolment table -->
		$enrolment.on('click', '.remove-users', function(e) {
			e.preventDefault();
			$('#enrolment-table').find('tr.item-type-include input:checked, tr.item-type-sits input:checked').each(function() {
				var usercode = $(this).val();
				var $tr = $(this).closest('tr');

				// update both hidden fields and table
				$('#enrolment-table').find('input:hidden[name=includeUsers][value='+ usercode + ']').remove();

				$('#enrolment-table').append($('<input type="hidden" name="excludeUsers" />').val(usercode));
				if ($tr.is('.item-type-sits')) {
					$tr.find('.source').html('<#noescape>${excludeIcon}</#noescape>');
				} else {
					$tr.find('.source').html('<#noescape>${pendingDeletionIcon}</#noescape>');
				}
				$tr.removeClass(function(i, css) {
					return (css.match(/\bitem-type-\S+/g) || []).join(' ');
				}).addClass('item-type-exclude');

				this.checked = '';
				alertPending();
			});
		});

		<#-- restore excluded user -->
		$enrolment.on('click', '.restore-users', function(e) {
			e.preventDefault();
			$('#enrolment-table').find('tr.item-type-exclude input:checked').each(function() {
				var usercode = $(this).val();
				var $tr = $(this).closest('tr');

				// update both hidden fields and table
				$('#enrolment-table').find('input:hidden[name=excludeUsers][value='+ usercode + ']').remove();
				$('#enrolment-table').append($('<input type="hidden" name="includeUsers" />').val(usercode));
				$tr.find('.source').html('<#noescape>${includeIcon}</#noescape>');

				$tr.removeClass(function(i, css) {
					return (css.match(/\bitem-type-\S+/g) || []).join(' ');
				}).addClass('item-type-include pending');

				this.checked = '';
				alertPending();
			});
		});
	});
	</script>
</#escape>