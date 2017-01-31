<#escape x as x?html>
<#import "attendance_variables.ftl" as attendance_variables />
<#import "attendance_macros.ftl" as attendance_macros />
<#import "*/modal_macros.ftl" as modal />

<script>
	(function ($) {
		$(function() {
			$('.fix-area').fixHeaderFooter();
		});
	} (jQuery));

</script>

<#if groupedPointMap?keys?size == 0>
	<p><em>No monitoring points found for this academic year.</em></p>
<#else>
<div class="recordCheckpointForm">

	<@attendance_macros.attendanceButtons />

	<div class="fix-area">
		<h1>Record attendance</h1>
		<h6><span class="muted">for</span> ${student.fullName}, ${department.name}</h6>

		<form id="recordAttendance" action="" method="post" class="dirty-check">
			<input type="hidden" name="returnTo" value="${returnTo}"/>
			<script>
				AttendanceRecording.bindButtonGroupHandler();
			</script>

			<#macro controls pointCheckpointPair>
				<#local point = pointCheckpointPair._1() />
				<#if pointCheckpointPair._2()??>
					<@attendance_macros.checkpointSelect
						id="checkpointMap-${point.id}"
						name="checkpointMap[${point.id}]"
						department=department
						student=student
						checkpoint=pointCheckpointPair._2()
						point=point
					/>
				<#else>
					<@attendance_macros.checkpointSelect
						id="checkpointMap-${point.id}"
						name="checkpointMap[${point.id}]"
						department=department
						student=student
						point=point
					/>
				</#if>
				<#if mapGet(attendanceNotes, point)??>
					<#assign note = mapGet(attendanceNotes, point) />
					<#if note.hasContent>
						<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note edit" title="Edit attendance note" href="<@routes.attendance.noteEdit academicYear.startYear?c student point />?dt=${.now?string('iso')}">
							<i class="icon-edit attendance-note-icon"></i>
						</a>
					<#else>
						<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.attendance.noteEdit academicYear.startYear?c student point />">
							<i class="icon-edit attendance-note-icon"></i>
						</a>
					</#if>
				<#else>
					<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.attendance.noteEdit academicYear.startYear?c student point />">
						<i class="icon-edit attendance-note-icon"></i>
					</a>
				</#if>
				<#if point.pointType.dbValue == "meeting">
					<a class="meetings" title="Meetings with this student" href="<@routes.attendance.profileMeetings student academicYear.startYear?c point />"><i class="icon-info-sign icon-fixed-width"></i></a>
				<#elseif point.pointType.dbValue == "smallGroup">
					<a class="small-groups" title="Small group teaching events for this student" href="<@routes.attendance.profileGroups student academicYear.startYear?c point />"><i class="icon-info-sign icon-fixed-width"></i></a>
				<#else>
					<i class="icon-fixed-width"></i>
				</#if>
			</#macro>

			<#macro errorsAndScript pointCheckpointPair>
				<#local point = pointCheckpointPair._1() />
				<@spring.bind path="command.checkpointMap[${point.id}]">
					<#if status.error>
						<div class="text-error"><@f.errors path="command.checkpointMap[${point.id}]" cssClass="error"/></div>
					</#if>
				</@spring.bind>
				<script>
					AttendanceRecording.createButtonGroup('#checkpointMap-${point.id}');
				</script>
			</#macro>

			<#list attendance_variables.monitoringPointTermNames as term>
				<#if groupedPointMap[term]??>
					<@attendance_macros.groupedPointsBySection groupedPointMap term; pointCheckpointPair>
						<#assign point = pointCheckpointPair._1() />
						<div class="span12">
							<#if mapGet(reportedPointMap, point)??>
								<#assign reportedTerm = mapGet(reportedPointMap, point) />
								<div class="pull-right">
									<#if pointCheckpointPair._2()??>
										<@attendance_macros.checkpointLabel department=department student=student checkpoint=pointCheckpointPair._2() point=point />
									<#else>
										<@attendance_macros.checkpointLabel department=department student=student point=point />
									</#if>
								</div>
								${point.name}
								(<a class="use-tooltip" data-html="true" title="
									<@fmt.wholeWeekDateFormat
										point.startWeek
										point.endWeek
										point.scheme.academicYear
									/>
								"><@fmt.monitoringPointWeeksFormat
									point.startWeek
									point.endWeek
									point.scheme.academicYear
									department
								/></a>)
								<div class="alert alert-info">
									<i class="icon-ban-circle"></i>
									This student's attendance for ${reportedTerm.termTypeAsString}
									(<@fmt.date date=reportedTerm.startDate relative=false includeTime=false shortMonth=true /> - <@fmt.date date=reportedTerm.endDate relative=false includeTime=false shortMonth=true />)
									has already been uploaded to SITS:eVision.
								</div>
							<#else>
								<div class="pull-right">
									<@controls pointCheckpointPair/>
								</div>
								${point.name}
								(<a class="use-tooltip" data-html="true" title="
									<@fmt.wholeWeekDateFormat
										point.startWeek
										point.endWeek
										point.scheme.academicYear
									/>
								"><@fmt.monitoringPointWeeksFormat
									point.startWeek
									point.endWeek
									point.scheme.academicYear
									department
								/></a>)
								<@errorsAndScript pointCheckpointPair />
							</#if>
						</div>
					</@attendance_macros.groupedPointsBySection>
				</#if>
			</#list>

			<#list monthNames as month>
				<#if groupedPointMap[month]??>
					<@attendance_macros.groupedPointsBySection groupedPointMap month; pointCheckpointPair>
						<#assign point = pointCheckpointPair._1() />
						<div class="span12">
							<#if mapGet(reportedPointMap, point)??>
								<#assign reportedTerm = mapGet(reportedPointMap, point) />
								<div class="pull-right">
									<#if pointCheckpointPair._2()??>
											<@attendance_macros.checkpointLabel department=department student=student checkpoint=pointCheckpointPair._2() point=point />
										<#else>
										<@attendance_macros.checkpointLabel department=department student=student point=point />
									</#if>
								</div>
								${point.name}
								(<@fmt.interval point.startDate point.endDate />)
								<div class="alert alert-info">
									<i class="icon-ban-circle"></i>
									This student's attendance for ${reportedTerm.termTypeAsString}
									(<@fmt.date date=reportedTerm.startDate relative=false includeTime=false shortMonth=true /> - <@fmt.date date=reportedTerm.endDate relative=false includeTime=false shortMonth=true />)
									has already been uploaded to SITS:eVision.
								</div>
							<#else>
								<div class="span12">
									<div class="pull-right">
										<@controls pointCheckpointPair/>
									</div>
									${point.name}
									(<@fmt.interval point.startDate point.endDate />)
									<@errorsAndScript pointCheckpointPair />
								</div>
							</#if>
						</div>
					</@attendance_macros.groupedPointsBySection>
				</#if>
			</#list>

			<div class="submit-buttons fix-footer save-row">
				<div class="pull-right">
					<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
					<a class="btn dirty-check-ignore" href="${returnTo}">Cancel</a>
				</div>
			</div>
		</form>
	</div>
</div>
</#if>

<div id="meetings-modal" class="modal hide fade" style="display:none;">
	<@modal.header>
		<h3>Meetings</h3>
	</@modal.header>
	<@modal.body></@modal.body>
</div>
<div id="small-groups-modal" class="modal hide fade" style="display:none;"></div>

</#escape>
