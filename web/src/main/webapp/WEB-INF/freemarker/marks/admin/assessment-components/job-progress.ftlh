<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

<div class="deptheader">
  <h1>Record marks</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<#assign recordMarksSkipImportAction><@routes.marks.assessmentcomponent_marks_skipImport assessmentComponent upstreamAssessmentGroup /></#assign>
<@f.form action=recordMarksSkipImportAction?markup_string cssClass="dirty-check" method="post">
  <@f.hidden name="jobId" value=jobId />

  <h2>Importing student data</h2>

  <div class="alert alert-info">
    <div class="progress">
      <div class="progress-bar progress-bar-striped active" style="width: ${jobProgress!0}%;"></div>
    </div>
    <p class="job-status">${jobStatus!"Waiting for job to start"}</p>
  </div>
  <p>
    Tabula is currently importing fresh data for the students you selected from SITS.
    You can <a href="#" data-toggle="modal" data-target="#student-import-dates">view the last import date for each student</a>.
    If you wish you can skip this import and proceed to enter component marks.
  </p>
  <#if oldestImport??>
    <p>
      If you skip the import, the marks shown will be from data available in SITS at
      <@fmt.date date=oldestImport capitalise=false at=true relative=true />.
    </p>
  </#if>
  <@modal.modal id="student-import-dates">
    <@modal.wrapper>
      <@modal.body>
        <table class="table table-condensed table-striped table-hover table-sortable">
          <thead>
          <tr>
            <th class="sortable">Name</th>
            <th class="sortable">Last imported date</th>
          </tr>
          </thead>
          <tbody>
          <#list studentLastImportDates as studentDate>
            <tr>
              <td>${studentDate._1()}</td>
              <td data-sortby="${studentDate._2().millis}"><@fmt.date studentDate._2() /></td>
            </tr>
          </#list>
          </tbody>
        </table>
      </@modal.body>
    </@modal.wrapper>
  </@modal.modal>

  <button class="btn btn-primary" type="submit">Skip import and record component marks</button>
</@f.form>

<script nonce="${nonce()}">
  jQuery(function ($) {
    var updateProgress = function () {
      $.ajax({
        type: "POST",
        url: '<@routes.marks.assessmentcomponent_marks_progress assessmentComponent upstreamAssessmentGroup />',
        data: {'jobId': '${jobId}'},
        error: function (jqXHR, textstatus, message) {
          if (textstatus === "timeout") {
            updateProgress();
          } else {
            // not a timeout - some other JS error - advise the user to reload the page
            var $progressContainer = $('.alert').removeClass('alert-info').addClass('alert-warning');
            $progressContainer.find('.progress-bar').addClass("progress-bar-danger");
            var messageEnd = jqXHR.status === 403 ? ", it appears that you have signed out. Please refresh this page." : ". Please refresh this page.";
            $progressContainer.find('.job-status').html("Unable to check the progress of your import" + messageEnd);
          }
        },
        success: function (data) {
          if (data.finished) {
            window.location = '<@routes.marks.assessmentcomponent_marks_importComplete assessmentComponent upstreamAssessmentGroup />';
          } else {
            if (data.progress) {
              $('.progress .progress-bar').css('width', data.progress + '%');
            }
            if (data.status) {
              $('.job-status').html(data.status);
            }
            setTimeout(updateProgress, 5 * 1000);
          }
        },
        timeout: 5000
      });
    };
    updateProgress();
  });
</script>
