<#escape x as x?html>
  <#if submission.canAddNote>
    <@f.form id="mitCircsNoteForm" method="POST" modelAttribute="addCommand" class="dirty-check double-submit-protection" enctype="multipart/form-data">
      <div class="panel panel-default panel-form">
        <div class="panel-heading">
          <div class="pull-right form-inline">
            <@bs3form.filewidget
              basename="file"
              labelText=""
              types=[]
              multiple=true
              required=false
              customHelp=" "
            />

            <button type="submit" class="btn btn-sm btn-primary">Save</button>
          </div>
          <label class="control-label" for="text">Add a note</label>
        </div>
        <div class="panel-body">
          <@f.textarea path="text" class="form-control" rows="5" aria\-label="Add a note" />
        </div>
      </div>
    </@f.form>
  </#if>

  <#macro render_note note>
    <div class="panel panel-default">
      <div class="panel-heading">
        <h3 class="panel-title">
          <span class="date">
            ${note.creator.fullName},
            <@fmt.date date=note.createdDate />
            <#if note.lastModified != note.createdDate>
              (last updated <@fmt.date date=note.lastModified />)
            </#if>
          </span>

          <span class="pull-right">
            <#-- TODO Edit note? -->
            <#local deleteUrl><@routes.mitcircs.deleteNote note /></#local>
            <@f.form method="post" action=deleteUrl modelAttribute="" cssClass="form-inline double-submit-protection">
              <button type="submit" class="btn btn-xs btn-link" data-toggle="confirm-submit" data-message="Are you sure you want to delete this note?" aria-label="Delete note">
                <i title="Delete note" class="use-tooltip fal fa-times-circle"></i>
              </button>
            </@f.form>
          </span>
        </h3>
      </div>
      <div class="panel-body">
        <#noescape>${note.formattedText}</#noescape>
      </div>
      <#if note.attachments?has_content>
        <ul class="list-group">
          <#list note.attachments as attachment>
            <#local mimeTypeDetectionResult = mimeTypeDetector(attachment) />
            <li id="attachment-${attachment.id}" class="list-group-item attachment">
              <@fmt.file_type_icon mimeTypeDetectionResult.mediaType />
              <a href="<@routes.mitcircs.renderNoteAttachment note attachment />" <#if mimeTypeDetectionResult.serveInline>data-inline="true"</#if>><#compress>${attachment.name}</#compress></a>
            </li>
          </#list>
        </ul>
      </#if>
    </div>
  </#macro>

  <div data-count="${notes?size}" id="mc-notes-list">
    <#list notes?reverse as note>
      <@render_note note />
    </#list>
  </div>

  <script type="text/javascript">
    jQuery(function($) {
      // be sure to bind the confirm-submit handler before other handlers on submit buttons
      $('a[data-toggle~="confirm-submit"][data-message], :button[data-toggle~="confirm-submit"][data-message]', '.panel').on('click', function confirmBeforeSubmit(event) {
        var $button = $(this);
        // eslint-disable-next-line no-alert
        if (!window.confirm($button.data('message'))) {
          event.preventDefault();
          event.stopImmediatePropagation();
        }
      });
    });
  </script>
</#escape>