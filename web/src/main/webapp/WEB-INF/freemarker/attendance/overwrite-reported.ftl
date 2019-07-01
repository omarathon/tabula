<#import "attendance_macros.ftl" as attendance_macros />

<#escape x as x?html>
  <div class="deptheader">
    <h1>Overwrite reported attendance</h1>
    <h5 class="with-related"><span class="muted">for</span> ${student.fullName}, ${department.name}</h5>
  </div>

  <p>
    <#if checkpoint??>
      ${checkpoint.state.description}:
    <#else>
      Not recorded:
    </#if>

    ${point.name}

    <#if point.scheme.pointStyle.dbValue == "week">
      (<@fmt.wholeWeekDateFormat
        point.startWeek
        point.endWeek
        point.scheme.academicYear
      />)
    <#else>
      (<@fmt.interval point.startDate point.endDate />)
    </#if>
  </p>

  <#if checkpoint??>
    <@attendance_macros.checkpointDescription department=department checkpoint=checkpoint point=point student=student />
  </#if>

  <#if monitoringPointReport??>
    <p>
      Reported to SITS by ${monitoringPointReport.reporter}, <@fmt.date monitoringPointReport.pushedDate!monitoringPointReport.createdDate />
    </p>

    <div class="alert alert-danger">
      <p>If a reported monitoring point is changed without the totals that have been reported to SITS being changed by the Student Records team,
      the values will get out of sync; SITS records the number of missed (unauthorised) points and changing the data in Tabula should only
      happen once Student Records have confirmed that they have updated the records in SITS.</p>
    </div>
  </#if>

  <div class="recordCheckpointForm">
    <@attendance_macros.attendanceButtons />

    <@f.form method="post" modelAttribute="command">
      <script>
        AttendanceRecording.bindButtonGroupHandler();
      </script>

      <div class="striped-section collapsible expanded">
        <h2 class="section-title with-contents">
          <a class="collapse-trigger icon-container" href="#"><#if monitoringPointReport??>
            ${monitoringPointReport.monitoringPeriod},
            ${monitoringPointReport.academicYear.toString}
          </#if></a>
        </h2>
        <div class="striped-section-contents">
          <div class="item-info point clearfix">
            <div class="pull-right">
              <#if checkpoint??>
                <@attendance_macros.checkpointSelect
                  id="checkpointMap-${point.id}"
                  name="state"
                  department=department
                  student=student
                  checkpoint=checkpoint
                  point=point
                />
              <#else>
                <@attendance_macros.checkpointSelect
                  id="checkpointMap-${point.id}"
                  name="state"
                  department=department
                  student=student
                  point=point
                />
              </#if>

              <#if note??>
                <#if note.hasContent>
                  <a id="attendanceNote-${student.universityId}-${point.id}" class="btn btn-default use-tooltip attendance-note edit" title="Edit attendance note"
                     aria-label="Edit attendance note" href="<@routes.attendance.noteEdit point.scheme.academicYear student point />?dt=${.now?string('iso')}&returnTo=<@routes.attendance.profileOverwritePoint student point />">
                    <i class="fa fa-pencil-square-o attendance-note-icon"></i>
                  </a>
                <#else>
                  <a id="attendanceNote-${student.universityId}-${point.id}" class="btn btn-default use-tooltip attendance-note" title="Add attendance note"
                     aria-label="Add attendance note" href="<@routes.attendance.noteEdit point.scheme.academicYear student point />?returnTo=<@routes.attendance.profileOverwritePoint student point />">
                    <i class="fa fa-pencil-square-o attendance-note-icon"></i>
                  </a>
                </#if>
              <#else>
                <a id="attendanceNote-${student.universityId}-${point.id}" class="btn btn-default  use-tooltip attendance-note" title="Add attendance note"
                   aria-label="Add attendance note" href="<@routes.attendance.noteEdit point.scheme.academicYear student point />?returnTo=<@routes.attendance.profileOverwritePoint student point />">
                  <i class="fa fa-pencil-square-o attendance-note-icon"></i>
                </a>
              </#if>
            </div>
            ${point.name}
            (<@fmt.interval point.startDate point.endDate />)
            <@spring.bind path="state">
              <#if status.error>
                <div class="text-error"><@f.errors path="state" cssClass="error"/></div>
              </#if>
            </@spring.bind>
            <script>
              AttendanceRecording.createButtonGroup('#checkpointMap-${point.id}');
            </script>
          </div>
        </div>
      </div>

      <div class="submit-buttons save-row">
        <input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
        <#if monitoringPointReport??>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.profiles.profile_attendance monitoringPointReport.studentCourseDetails monitoringPointReport.academicYear />">Cancel</a>
        </#if>
      </div>
    </@f.form>
  </div>
</#escape>
