<#import "*/modal_macros.ftlh" as modal />
<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>
  <#if studentCourseDetails?has_content>
    <div class="clearfix">
      <p class="pull-right">
        <@fmt.bulk_email_students students=students />
      </p>
    </div>
    <@student_macros.tableWithMeetingsColumn items=studentCourseDetails meetingsMap=meetingsMap showSelectStudents=true/>
    <@modal.modal id="meeting-modal"></@modal.modal>
    <!-- TODO Changes required in BulkMeetingRecordController to make it working based on member para (if user should be able to see it). Hiding it for others for time being as it throws permission exception if other users clicks it)-->
    <#if !member?has_content>
      <div class="submit-buttons fix-footer">
        <section class="meetings">
          <a tabindex="0" role="button" data-href="<@routes.profiles.create_bulk_meeting_record relationshipType />" class="btn btn-default new-meeting-record disabled">Record meeting for
            selected students</a>
          <a tabindex="0" role="button" data-href="<@routes.profiles.schedule_bulk_meeting_record relationshipType />" class="btn btn-default schedule-meeting-record disabled">Schedule meeting for
            selected students</a>
        </section>
      </div>
    </#if>

  <#else>
    <p class="alert alert-info">No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
  </#if>

</#escape>
