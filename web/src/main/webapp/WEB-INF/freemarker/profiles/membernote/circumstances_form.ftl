<#escape x as x?html>
  <#if memberNoteSuccess??>
  <#else>
    <div class="alert alert-info">Extenuating circumstances are visible to students.</div>
    <@f.form id="edit-member-note-form" method="post" enctype="multipart/form-data" action="" modelAttribute="command" class="double-submit-protection">

      <@bs3form.labelled_form_group path="title" labelText="Title">
        <@f.input type="text" path="title" cssClass="form-control" maxlength="255" />
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group path="startDate" labelText="Start date">
        <@f.input id="startDate" path="startDate" cssClass="date-picker form-control" />
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group path="endDate" labelText="End date">
        <@f.input id="endDate" path="endDate" cssClass="date-picker form-control" />
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group path="note" labelText="Note">
        <@f.textarea path="note" cssClass="form-control" rows="5" cssStyle="height: 150px;" />
      </@bs3form.labelled_form_group>

      <@bs3form.attachmentsList
        path="attachedFiles"
        labelText="Attached files"
        attachedFiles=command.attachedFiles
        help="This is a list of file attachments for this administrative note. Click the remove link next to a document to delete it."
      />

      <#assign fileTypes=command.attachmentTypes />
      <@bs3form.filewidget basename="file" types=fileTypes />

    </@f.form>

  </#if>
</#escape>
