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
          <@spring.hasBindErrors name="command">
            <#if errors.hasErrors()>
              <div class="alert alert-danger">
                <#-- show global errors (problems with extracting the spreadsheet) -->
                <h3>Some problems need fixing</h3>

                <p>Your spreadsheet hasn't been processed because of the following issues. Please fix the problems and try again.</p>

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
          </@spring.hasBindErrors>

          <p>
            The spreadsheet must be saved in CSV format with two columns. Put the student's University ID in the first column and
            record their attendance in the second. The value for attendance must be one of the following:
          </p>
          <ul>
            <li><code>attended</code> </li>
            <li><code>authorised</code> – you can only set this attendance value if you have created an attendance note in advance for those students with authorisation to miss this point</li>
            <li><code>unauthorised</code> </li>
            <li><code>not-recorded</code> </li>
          </ul>

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
