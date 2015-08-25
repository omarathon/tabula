<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<#import "*/_profile_link.ftl" as pl />

<script type="text/javascript">
(function ($) {
	$(function() {
		$('.select-all').change(function(e) {
			$('.attendees').selectDeselectCheckboxes(this);
		});

	});
} (jQuery));
</script>

<div id="addAdditional-modal" class="modal fade"></div>

<div class="recordCheckpointForm">
	<div style="display:none;" class="forCloning">
		<div class="btn-group" data-toggle="radio-buttons">
			<button type="button" class="btn btn-default" data-state="">
				<i class="fa fa-fw fa-minus" title="Set to 'Not recorded'"></i>
			</button>
			<button type="button" class="btn btn-default btn-unauthorised<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>" data-state="unauthorised">
				<i class="fa fa-fw fa-times" title="Set to 'Missed (unauthorised)'"></i>
			</button>
			<button type="button" class="btn btn-default btn-authorised" data-state="authorised">
				<i class="fa fa-fw fa-times-circle-o" title="Set to 'Missed (authorised)'"></i>
			</button>
			<button type="button" class="btn btn-default btn-attended<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>" data-state="attended">
				<i class="fa fa-fw fa-check" title="Set to 'Attended'"></i>
			</button>
		</div>
	</div>

	<div id="profile-modal" class="modal fade profile-subset"></div>
	<div class="fix-area">
		<div class="deptheader">
			<h1>Record attendance</h1>
			<h4 class="with-related"><span class="muted">for</span>
				${command.event.group.groupSet.module.code?upper_case} ${command.event.group.groupSet.nameWithoutModulePrefix},
				${command.event.group.name}</h4>
		</div>

		<div id="print-modal" class="modal fade">
			<@modal.wrapper>
				<@modal.header>
					<h3 class="modal-title">Print register</h3>
				</@modal.header>
				<form method="get" action="<@routes.groups.registerPdf command.event />" style="margin-bottom: 0;">
					<input type="hidden" name="week" value="${command.week?c}" />
					<@modal.body>
						<p>Generate a PDF of this register so that it can be printed.</p>

						<@bs3form.checkbox>
							<input type="hidden" name="_showPhotos" value="" />
							<input type="checkbox" name="showPhotos" value="true" checked />
							Show student photos
						</@bs3form.checkbox>

						<@bs3form.labelled_form_group "" "Name display">
							<@bs3form.radio>
								<input type="radio" name="displayName" value="name" checked />
								Show student name only
							</@bs3form.radio>
							<@bs3form.radio>
								<input type="radio" name="displayName" value="id" />
								Show University ID only
							</@bs3form.radio>
							<@bs3form.radio>
								<input type="radio" name="displayName" value="both" />
								Show both name and University ID
							</@bs3form.radio>
						</@bs3form.labelled_form_group>

						<@bs3form.labelled_form_group "" "Fill area">
							<@bs3form.radio>
								<input type="radio" name="displayCheck" value="checkbox" checked />
								Include checkbox
							</@bs3form.radio>
							<@bs3form.radio>
								<input type="radio" name="displayCheck" value="line" />
								Include signature line
							</@bs3form.radio>
						</@bs3form.labelled_form_group>
					</@modal.body>
					<@modal.footer>
						<button type="submit" class="btn btn-primary">Save as PDF for printing</button>
						<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Close</button>
					</@modal.footer>
				</form>
			</@modal.wrapper>
		</div>
		<div class="pull-right">
			<button type="button" class="btn btn-default" data-toggle="modal" data-target="#print-modal">Print</button>
		</div>

		<h6>
			${command.event.day.name} <@fmt.time event.startTime /> - <@fmt.time event.endTime />, Week ${command.week}
			<#if command.event.tutors.users?has_content>
				<br /><@fmt.p number=command.event.tutors.users?size singular="Tutor" shownumber=false/>: <#list command.event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>
			</#if>
		</h6>


		<div class="fix-header">
			<div class="row check-all">
				<div class="col-md-12"><div class="bg clearfix"><div class="col-md-12">
					<span class="studentsLoadingMessage">
						<i class="fa fa-spinner fa-spin"></i><em> Loading&hellip;</em>
					</span>
					<script type="text/javascript">
						jQuery(function($){
							$('.studentsLoadingMessage').hide();
						});
					</script>
					<div class="pull-left">
						<div class="form-inline">
							<label for="additionalStudentQuery">Add student:</label>
							<span class="profile-search input-group" data-target="<@routes.groups.students_json command.event.group.groupSet />?excludeEvent=${command.event.id}&excludeWeek=${command.week}" data-form="<@routes.groups.addAdditionalStudent command.event command.week />" data-modal="#addAdditional-modal">
								<input type="hidden" name="additionalStudent" />
								<input style="width: 300px;" class="form-control" type="text" name="query" value="" id="additionalStudentQuery" placeholder="Search for a student to add&hellip;" />
								<span class="input-group-btn">
									<button class="btn btn-default" type="button">
										<i class="fa fa-search"></i>
									</button>
								</span>
							</span>

							<#assign helpText>
								<p>If a student not normally in this group has attended or will be attending this session, you can search for them here.</p>

								<p>Only students registered to small groups in this module are included in the search results.</p>
							</#assign>
							<a href="#"
							   class="use-popover"
							   data-title="Adding a student not normally present to an event occurrence"
							   data-trigger="click"
							   data-placement="bottom"
							   data-html="true"
							   data-content="${helpText}"><i class="icon-question-sign icon-fixed-width"></i></a>
						</div>
					</div>
					<div class="pull-right" style="display: none;">
						<span class="checkAllMessage">
							Record all attendance
							<#assign popoverContent>
								<p><i class="fa fa-minus fa-fw"></i> Not recorded</p>
									<p><i class="fa fa-times fa-fw unauthorised"></i> Missed (unauthorised)</p>
									<p><i class="fa fa fa-times-circle-o fa-fw authorised"></i> Missed (authorised)</p>
									<p><i class="fa fa-check fa-fw attended"></i> Attended</p>
							</#assign>
							<a class="use-popover"
							   data-title="Key"
							   data-placement="bottom"
							   data-container="body"
							   data-content='${popoverContent}'
							   data-html="true">
								<i class="fa fa-question-circle"></i>
							</a>
							</span>
						<div class="btn-group">
							<button type="button" class="btn btn-default">
								<i class="fa fa-minus fa-fw" title="Set all to 'Not recorded'"></i>
							</button>
							<button type="button" class="btn btn-default btn-unauthorised<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>">
								<i class="fa fa-times fa-fw" title="Set all to 'Missed (unauthorised)'"></i>
							</button>
							<button type="button" class="btn btn-default btn-authorised">
								<i class="fa fa fa-times-circle-o fa-fw" title="Set all to 'Missed (authorised)'"></i>
							</button>
							<button type="button" class="btn btn-default btn-attended<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>">
								<i class="fa fa-check fa-fw" title="Set all to 'Attended'"></i>
							</button>
						</div>
						<#if features.attendanceMonitoringNote>
							<a style="visibility: hidden" class="btn btn-default"><i class="fa fa-pencil-square-o"></i></a>
						</#if>
					</div>
				</div></div></div>
			</div>
		</div>

		<#if !command.members?has_content && !command.manuallyAddedUniversityIds?has_content && !command.additionalStudent?has_content>
			<p><em>There are no students allocated to this group.</em></p>
		</#if>

		<#macro studentRow student added_manually linked_info={}>
			<div class="row" id="student-${student.universityId}">
				<div class="col-md-12"><div class="bg clearfix">
					<div class="col-md-8">
						<@fmt.member_photo student "tinythumbnail" true />
						${student.fullName}
						<@pl.profile_link student.universityId />

						<#if added_manually>
							<#assign popoverText>
								<p>${student.fullName} has been manually added to this event.</p>

								<#if linked_info.replacesAttendance??>
									<p>Attending instead of
									${linked_info.replacesAttendance.occurrence.event.group.groupSet.name}, ${linked_info.replacesAttendance.occurrence.event.group.name}:
									${linked_info.replacesAttendance.occurrence.event.day.name} <@fmt.time linked_info.replacesAttendance.occurrence.event.startTime /> - <@fmt.time linked_info.replacesAttendance.occurrence.event.endTime />, Week ${linked_info.replacesAttendance.occurrence.week}.
									</p>
								</#if>

								<button class="btn btn-mini btn-danger" type="button" data-action="remove" data-student="${student.universityId}">Remove from this occurrence</button>
							</#assign>

							<a class="use-popover" data-container="body" data-content="${popoverText}" data-html="true">
								<i class="icon-hand-up"></i>
							</a>
						</#if>

						<#if linked_info.replacedBy??>
							<#list linked_info.replacedBy as replaced_by>
								<i class="icon-link use-tooltip" data-container="body" title="<#compress>
								Replaced by attendance at
								${replaced_by.occurrence.event.group.groupSet.name}, ${replaced_by.occurrence.event.group.name}:
								${replaced_by.occurrence.event.day.name} <@fmt.time replaced_by.occurrence.event.startTime /> - <@fmt.time replaced_by.occurrence.event.endTime />, Week ${replaced_by.occurrence.week}.
							</#compress>"></i>
							</#list>
						</#if>
					</div>
					<div class="col-md-4">
						<div class="pull-right">
							<#local hasState = mapGet(command.studentsState, student.universityId)?? />
							<#if hasState>
								<#local currentState = mapGet(command.studentsState, student.universityId) />
							</#if>
							<#if hasState>
								<div class="hidden-desktop visible-print">
									<span class="label">${currentState.description}</span><br />
									${(command.attendanceMetadata(student.universityId))!}
								</div>
							</#if>
							<select id="studentsState-${student.universityId}" name="studentsState[${student.universityId}]" data-universityid="${student.universityId}">
								<option value="" <#if !hasState>selected</#if>>Not recorded</option>
								<#list allCheckpointStates as state>
									<option value="${state.dbValue}" <#if hasState && currentState.dbValue == state.dbValue>selected</#if>>${state.description}</option>
								</#list>
							</select>
							<#if features.attendanceMonitoringNote>
								<#local hasNote = mapGet(mapGet(command.attendanceNotes, student), command.occurrence)?? />
								<#if hasNote>
									<#local note = mapGet(mapGet(command.attendanceNotes, student), command.occurrence) />
									<#if note.hasContent>
										<a
											id="attendanceNote-${student.universityId}-${command.occurrence.id}"
											class="btn btn-default use-tooltip attendance-note edit"
											title="Edit attendance note"
											data-container="body"
											href="<@routes.groups.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
										>
											<i class="fa fa-pencil-square-o attendance-note-icon"></i>
										</a>
									<#else>
										<a
											id="attendanceNote-${student.universityId}-${command.occurrence.id}"
											class="btn btn-default use-tooltip attendance-note"
											title="Add attendance note"
											data-container="body"
											href="<@routes.groups.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
										>
											<i class="fa fa-pencil-square-o attendance-note-icon"></i>
										</a>
									</#if>
								<#else>
									<a
										id="attendanceNote-${student.universityId}-${command.occurrence.id}"
										class="btn btn-default use-tooltip attendance-note"
										title="Add attendance note"
										data-container="body"
										href="<@routes.groups.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
									>
										<i class="fa fa-pencil-square-o attendance-note-icon"></i>
									</a>
								</#if>
							</#if>
						</div>
					</div>

					<@spring.bind path="command.studentsState[${student.universityId}]">
						<#list status.errorMessages as err>${err}</#list>
						<div class="text-error"><@f.errors path="studentsState[${student.universityId}]" cssClass="error"/></div>
					</@spring.bind>
				</div></div>

				<script type="text/javascript">
					AttendanceRecording.createButtonGroup('#studentsState-${student.universityId}');
					AttendanceRecording.wireButtons('#student-${student.universityId}');
				</script>
			</div>
		</#macro>

		<div class="attendees">
			<form id="recordAttendance" action="" method="post" data-occurrence="${command.occurrence.id}">
				<script type="text/javascript">
					AttendanceRecording.bindButtonGroupHandler(true);
				</script>

				<#if command.additionalStudent??>
					<#assign already_in = false />
					<#list command.members as student>
						<#if student.universityId == command.additionalStudent.universityId>
							<#assign already_in = true />
						</#if>
					</#list>

					<#if !already_in><@studentRow command.additionalStudent true command.linkedAttendance /></#if>
				</#if>

				<#list command.members as student>
					<#if !(command.removeAdditionalStudent??) || command.removeAdditionalStudent.universityId != student.universityId>
						<@studentRow student command.manuallyAddedUniversityIds?seq_contains(student.universityId) mapGet(command.attendances, student) />
					</#if>
				</#list>

				<#if command.members?has_content || command.manuallyAddedUniversityIds?has_content>

					<div class="fix-footer submit-buttons">
						<div class="pull-right">
							<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
							<a class="btn btn-default" href="${returnTo}">Cancel</a>
						</div>
						<div class="pull-left checkpoints-message alert alert-info" style="display: none;">
							Saving this attendance will set monitoring points for
							<span class="students use-popover" data-placement="top" data-container="body" data-content=" " data-html="true"></span>
							as attended.
						</div>
					</div>

				</#if>
			</form>
		</div>

	</div>
</div>
</#escape>