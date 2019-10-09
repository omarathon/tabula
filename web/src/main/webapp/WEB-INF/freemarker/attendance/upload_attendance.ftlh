<#import "*/modal_macros.ftlh" as modal />

<@f.form action=uploadUrl method="post" enctype="multipart/form-data" style="margin-bottom: 0">

    <@modal.wrapper ajax>
        <#if ajax>
            <@modal.header>
              <h3 class="modal-title">Upload attendance from CSV</h3>
            </@modal.header>
        <#else>
          <h1>Upload attendance from CSV</h1>
          <h5><span class="muted">for</span> ${command.templatePoint.name}</h5>
        </#if>

        <@modal.body enabled=ajax>

          <#if errors.hasErrors()>
            <div class="alert alert-danger">
              <#-- show global errors (problems with extracting the spreadsheet) -->
              <@f.errors cssClass="error" />
              <#list command.checkpointMap?keys as student>
                  <#list mapGet(command.checkpointMap, student)?keys as point>
                      <@spring.bind path="command.checkpointMap[${student.universityId}][${point.id}]">
                        <#if status.error>
                          ${student.universityId} &mdash; <@f.errors path="checkpointMap[${student.universityId}][${point.id}]" cssClass="error" />
                        </#if>
                      </@spring.bind>
                  </#list>
              </#list>
            </div>
          </#if>

          <p>
            Each row in the uploaded file should have 2 values: University ID and the attendance.
            Attendance should be specified as one of the following values:
          </p>
          <ul>
            <li>attended</li>
            <li>unauthorised</li>
            <li>not-recorded</li>
          </ul>
          It is not possible to record authorised absences using the CSV upload.
          <@bs3form.labelled_form_group "" "File">
            <input type="file" name="file.upload" />
          </@bs3form.labelled_form_group>
        </@modal.body>

        <@modal.footer enabled=ajax>
          <input class="btn btn-primary spinnable spinner-auto" type="submit" data-loading-text="Uploading&hellip;">
          <button class="btn btn-default" data-dismiss="modal">Cancel</button>
        </@modal.footer>
    </@modal.wrapper>
</@f.form>