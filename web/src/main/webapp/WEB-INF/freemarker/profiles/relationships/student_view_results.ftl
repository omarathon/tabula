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

  <script type="text/javascript" nonce="${nonce()}">

    (function ($) {
      var $buttons = $('a.new-meeting-record, a.schedule-meeting-record');
      var generateBulkRecordLink = function () {
        var $selectedCheckBoxes = $(".collection-checkbox:checkbox:checked");
        if ($(".collection-check-all").prop("checked")) {
          $selectedCheckBoxes = $(".collection-checkbox:checkbox");
        }
        if ($selectedCheckBoxes.length > 0) {
          $buttons.removeClass('disabled');
          $buttons.each(function () {
            var $button = $(this);
            var course = $.map($selectedCheckBoxes, function (checkbox) {
              return $(checkbox).data('student-course-details');
            });
            $button.attr("href", $button.data("href") + course);
          });
        } else $buttons.addClass('disabled');
      };

      $('.collection-check-all').on('change', function (e) {
        generateBulkRecordLink();
      });

      $('.collection-checkbox').on('change', function (e) {
        generateBulkRecordLink()
      });

    })(jQuery);
  </script>
</#escape>
