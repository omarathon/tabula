<#escape x as x?html>
	<#import "../attendance_variables.ftl" as attendance_variables />
	<#import "../attendance_macros.ftl" as attendance_macros />
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

		<form id="recordAttendance" action="" method="post">
			<input type="hidden" name="returnTo" value="${returnTo}"/>
			<script>
				AttendanceRecording.bindButtonGroupHandler();
			</script>

			<#macro controls pointCheckpointPair>
				<#local point = pointCheckpointPair._1() />
				<#if pointCheckpointPair._2()??>
					<@attendance_macros.checkpointSelect department=department student=student checkpoint=pointCheckpointPair._2() point=point />
				<#else>
					<@attendance_macros.checkpointSelect department=department student=student point=point />
				</#if>
				<#if mapGet(attendanceNotes, point)??>
					<#assign note = mapGet(attendanceNotes, point) />
					<#if note.note?has_content || note.attachment?has_content >
						<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Edit attendance note" href="<@routes.noteEdit academicYear.startYear?c student point />">
							<i class="icon-edit-sign attendance-note-icon"></i>
						</a>
					<#else>
						<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.noteEdit academicYear.startYear?c student point />">
							<i class="icon-edit attendance-note-icon"></i>
						</a>
					</#if>
				<#else>
					<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.noteEdit academicYear.startYear?c student point />">
						<i class="icon-edit attendance-note-icon"></i>
					</a>
				</#if>
				<#if point.pointType.dbValue == "meeting">
					<a class="meetings" title="Meetings with this student" href="<@routes.profileMeetings student academicYear.startYear?c point />"><i class="icon-info-sign icon-fixed-width"></i></a>
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
							<#if mapGet(reportedPointMap, point)>
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
									<i class="icon-ban-circle"></i> This student's attendance for this term has already been uploaded to SITS:eVision.
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
							<#if mapGet(reportedPointMap, point)>
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
									<i class="icon-ban-circle"></i> This student's attendance for this term has already been uploaded to SITS:eVision.
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
					<a class="btn" href="${returnTo}">Cancel</a>
				</div>
			</div>
		</form>
	</div>
</div>
</#if>

<div id="modal" class="modal hide fade" style="display:none;">
	<@modal.header>
		<h3>Meetings</h3>
	</@modal.header>
	<@modal.body></@modal.body>
</div>

</#escape>

<#--<#macro monitoringPointsByTerm term>-->
<#--<div class="striped-section">-->
	<#--<h2 class="section-title">${term}</h2>-->
<#--<#if command.nonReportedTerms?seq_contains(term) >-->
	<#--<div class="striped-section-contents">-->
	<#--<#list command.monitoringPointsByTerm[term] as point>-->
		<#--<div class="item-info row-fluid point">-->
			<#--<div class="span12">-->
				<#--<div class="pull-right">-->
				<#--<#local title>-->
				<#--<#if features.attendanceMonitoringNote>-->
				<#--<#local hasNote = mapGet(command.attendanceNotes, point)?? />-->
				<#--<#if hasNote>-->
				<#--<#local note = mapGet(command.attendanceNotes, point) />-->
				<#--<#noescape>-->
					<#--<p>${mapGet(command.checkpointDescriptions, point)?default("")}</p>-->
					<#--<p>${note.truncatedNote}</p>-->
				<#--</#noescape>-->
				<#--<#else>-->
					<#--<p>${mapGet(command.checkpointDescriptions, point)?default("")}</p>-->
				<#--</#if>-->
				<#--</#if>-->
				<#--</#local>-->
					<#--<select-->
							<#--id="checkpointMap-${point.id}"-->
							<#--name="checkpointMap[${point.id}]"-->
							<#--title="${title}"-->
							<#-->-->
					<#--<#local hasState = mapGet(command.checkpointMap, point)?? />-->
						<#--<option value="" <#if !hasState >selected</#if>>Not recorded</option>-->
						<#--<option value="unauthorised" <#if hasState && mapGet(command.checkpointMap, point).dbValue == "unauthorised">selected</#if>>Missed (unauthorised)</option>-->
						<#--<option value="authorised" <#if hasState && mapGet(command.checkpointMap, point).dbValue == "authorised">selected</#if>>Missed (authorised)</option>-->
						<#--<option value="attended" <#if hasState && mapGet(command.checkpointMap, point).dbValue == "attended">selected</#if>>Attended</option>-->
					<#--</select>-->

				<#--<#if features.attendanceMonitoringNote>-->
				<#--<#local hasNote = mapGet(command.attendanceNotes, point)?? />-->
				<#--<#if hasNote>-->
				<#--<#local note = mapGet(command.attendanceNotes, point) />-->
				<#--<#if note.note?has_content || note.attachment?has_content>-->
					<#--<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Edit attendance note" href="<@routes.editNote student=command.student point=point returnTo=((info.requestedUri!"")?url) />"><i class="icon-edit-sign attendance-note-icon"></i></a>-->
				<#--<#else>-->
					<#--<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.editNote student=command.student point=point returnTo=((info.requestedUri!"")?url) />"><i class="icon-edit attendance-note-icon"></i></a>-->
				<#--</#if>-->
				<#--<#else>-->
					<#--<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.editNote student=command.student point=point returnTo=((info.requestedUri!"")?url) />"><i class="icon-edit attendance-note-icon"></i></a>-->
				<#--</#if>-->
				<#--</#if>-->

				<#--<#if point.pointType?? && point.pointType.dbValue == "meeting">-->
					<#--<a class="meetings" title="Meetings with this student" href="<@routes.studentMeetings point command.student />"><i class="icon-info-sign icon-fixed-width"></i></a>-->
				<#--<#else>-->
					<#--<i class="icon-fixed-width"></i>-->
				<#--</#if>-->
				<#--</div>-->
			<#--${point.name} (<a class="use-tooltip" data-html="true" title="<@fmt.monitoringPointDateFormat point />"><@fmt.monitoringPointFormat point /></a>)-->
			<#--<@spring.bind path="command.checkpointMap[${point.id}]">-->
			<#--<#if status.error>-->
				<#--<div class="text-error"><@f.errors path="command.checkpointMap[${point.id}]" cssClass="error"/></div>-->
			<#--</#if>-->
			<#--</@spring.bind>-->
			<#--</div>-->
			<#--<script>-->
				<#--AttendanceRecording.createButtonGroup('#checkpointMap-${point.id}');-->
			<#--</script>-->
		<#--</div>-->
	<#--</#list>-->
	<#--</div>-->
<#--<#else>-->
	<#--<i class="icon-ban-circle"></i> This student's attendance for this term has already been uploaded to SITS:eVision.-->
<#--</#if>-->
<#--</div>-->
<#--</#macro>-->
