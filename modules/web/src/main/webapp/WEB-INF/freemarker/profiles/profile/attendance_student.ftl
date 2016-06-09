<#import "*/modal_macros.ftl" as modal />

<#import "../../attendance/attendance_variables.ftl" as attendance_variables />
<#import "../attendance/attendance_macros.ftl" as attendance_macros />
<#import "../../attendance/attendance_note_macros.ftl" as attendance_note_macros />

<#escape x as x?html>

<h1>Attendance</h1>

<#if hasMonitoringPointAttendancePermission>
	<#assign groupedPointMap=monitoringPointAttendanceCommandResult.attendanceMonitoringPointWithCheckPoint />
	<#if groupedPointMap?keys?size == 0>
		<div class="seminar-attendance-profile striped-section collapsible">
			<h3 class="section-title">Monitoring points</h3>
			<p><em>No monitoring points found for this academic year.</em></p>
		</div>
	<#else>
		<div class="monitoring-points-profile striped-section collapsible expanded">
			<h3 class="section-title">Monitoring points</h3>
			<div class="missed-info">
				<#if !monitoringPointAttendanceCommandResult.hasAnyMissedPoints>
					<#if isSelf>
						You have missed 0 monitoring points.
					<#else>
						${student.firstName} has missed 0 monitoring points.
					</#if>
				<#else>
					<#macro missedWarning term>
						<#if (monitoringPointAttendanceCommandResult.missedPointCountByTerm[term]?? && monitoringPointAttendanceCommandResult.missedPointCountByTerm[term] > 0)>
							<div class="missed">
								<i class="icon-warning-sign"></i>
								<#if isSelf>
									You have
								<#else>
								${student.firstName} has
								</#if>
								missed <@fmt.p monitoringPointAttendanceCommandResult.missedPointCountByTerm[term] "monitoring point" /> in ${term}
							</div>
						</#if>
					</#macro>
					<#list attendance_variables.monitoringPointTermNames as term>
						<@missedWarning term />
					</#list>
					<#list monthNames as month>
						<@missedWarning month />
					</#list>
				</#if>
			</div>

			<div class="striped-section-contents">
				<#list attendance_variables.monitoringPointTermNames as term>
					<#if groupedPointMap[term]??>
						<div class="item-info row-fluid term">
							<div>
								<h4 class="attendance-term">${term}</h4>
								<table class="table">
									<tbody>
										<#list groupedPointMap[term] as pointPair>
											<#assign point = pointPair._1() />
											<tr class="point">
												<td class="point" title="${point.name} <@fmt.wholeWeekDateFormat startWeek=point.startWeek endWeek=point.endWeek academicYear=point.scheme.academicYear stripHtml=true/>">
													${point.name} (<@fmt.wholeWeekDateFormat point.startWeek point.endWeek	point.scheme.academicYear/>)
												</td>
												<td class="state">
													<#if pointPair._2()??>
														<@attendance_macros.checkpointLabel department=point.scheme.department checkpoint=pointPair._2()/>
													<#else>
														<@attendance_macros.checkpointLabel department=point.scheme.department point=pointPair._1() student=student />
													</#if>
												</td>
											</tr>
										</#list>
									</tbody>
								</table>
							</div>
						</div>
					</#if>
				</#list>
				<#list monthNames as month>
					<#if groupedPointMap[month]??>
						<div class="item-info row-fluid term">
							<div>
								<h4 class="attendance-term">${month}</h4>
								<table class="table">
									<tbody>
										<#list groupedPointMap[month] as pointPair>
											<#assign point = pointPair._1() />
											<tr class="point">
												<td class="point" title="${point.name} (<@fmt.interval point.startDate point.endDate  true/>)">
													${point.name} (<@fmt.interval point.startDate point.endDate />)
												</td>
												<td class="state">
													<#if pointPair._2()??>
														<@attendance_macros.checkpointLabel department=point.scheme.department checkpoint=pointPair._2() />
													<#else>
														<@attendance_macros.checkpointLabel department=point.scheme.department point=pointPair._1() student=student />
													</#if>
												</td>
											</tr>
										</#list>
									</tbody>
								</table>
							</div>
						</div>
					</#if>
				</#list>
			</div>
		</div>
		<#assign allNotes = monitoringPointAttendanceCommandResult.allNotesWithSomeCheckPoints />
		<div class="monitoring-points-profile striped-section collapsible">
			<h3 class="section-title">Attendance notes</h3>
			<div class="attendance-note-info">
				<#if isSelf>
					You have ${allNotes?size} attendance notes.
				<#else>
				${student.firstName} has ${allNotes?size} attendance notes.
				</#if>
			</div>
			<div class="striped-section-contents">
				<div class="row-fluid">
					<#if (allNotes?size > 0)>
						<h4>Filter Options</h4>
						<form>
							<div class= "form-group checkbox-inline  checkpointState-checkbox checkpointState-all">
								<label><input type="checkbox" name="all" value="all"   checked />All</label>
							</div>
							<#list allCheckpointStates as state>
								<div class= "form-group checkbox-inline checkpointState-checkbox checkpointState-${state.dbValue}"  >
									<label><input  type="checkbox" name="${state.dbValue}" value="${state.description}"  align="left" checked />${state.description}</label>
								</div>
							</#list>
						</form>
						<@attendance_note_macros.allNotes notes=allNotes  />
						<#list monitoringPointAttendanceCommandResult.checkPointNotes?keys as state>
							<@attendance_note_macros.checkpointNotes  checkpointNoteList=monitoringPointAttendanceCommandResult.checkPointNotes[state] type=state />
						</#list>
						<@attendance_note_macros.unrecordedNotes monitoringPointNoteList=monitoringPointAttendanceCommandResult.notesWithoutCheckPoints  />
					</#if>
				</div>
			</div>
		</div>
	</#if>
	</#if>
<#if hasSeminarAttendancePermission>
	<#include "../../groups/students_group_attendance.ftl" />
</#if>

	<script>
		jQuery(function($) {
			$('.checkpointState-checkbox input').on('change', function() {
				var checkboxInput = $(this);
				//if event is for change of all checkbox
				//if all is checked, show all notes and hide individual categories
				if (checkboxInput.prop('name') == 'all' && checkboxInput.prop('checked')){
					$('.allNotes').show();
					//mark all others as checked when you select all
					var filterCheckboxes = $('.checkpointState-checkbox input')
					$.each(filterCheckboxes, function(element) {
						$(this).prop('checked','checked')
					});
					hideNoteStates()

				} else if (checkboxInput.prop('name') == 'all'){
					//if all deselected,  hide all notes and show/hide individual categories based on checkboxes
					$('.allNotes').hide();
					// check all checkboxes and show/hide based on that
					var filterCheckboxes = $('.checkpointState-checkbox input')
					$.each(filterCheckboxes, function(element) {
						var elmnt = $(this);
						var elmntClass = ".note-state." +  elmnt.prop('name');
						if( elmnt.prop('checked')) {
							$(elmntClass).show();
						} else {
							$(elmntClass).hide();
						}

					});

				} else {
					//if event is for change of other checkboxes (other than all)
					var checkboxInputClass = "." + checkboxInput.prop('name');
					var container = $(checkboxInputClass);
					var allCheckboxInput = $('.checkpointState-checkbox.checkpointState-all input');
					// case for other checkboxes
					if (checkboxInput.prop('checked') && !allCheckboxInput.prop('checked')){
						container.show();
					} else {
						container.hide();
					}
				}

			});

			function hideNoteStates() {
				var elements = $('.note-state');
				$.each(elements, function(element) {
					$(this).hide();
				});
			};
			// hide all individual category notes at start
			hideNoteStates();

		});
	</script>
</#escape>