<#escape x as x?html>
  <#-- TODO need to check in what states it's valid to add a note? -->
  <@f.form id="mitCircsNoteForm" method="POST" modelAttribute="addCommand" class="dirty-check double-submit-protection" enctype="multipart/form-data">
    <div class="panel panel-default panel-form">
      <div class="panel-heading">
        <button type="submit" class="btn btn-primary">Save</button>
        <label class="control-label" for="text">Add a note</label>
      </div>
      <div class="panel-body">
        <@f.textarea path="text" class="form-control" rows="5" aria\-label="Add a note" />
      </div>
    </div>
  </@f.form>

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
                <i title="Delete note" class="icon-tooltip fal fa-times-circle"></i>
              </button>
            </@f.form>
          </span>
        </h3>
      </div>
      <div class="panel-body">
        <#noescape>${note.formattedText}</#noescape>
      </div>
    </div>
  </#macro>

  <div data-count="${notes?size}" id="mc-notes-list">
    <#list notes?reverse as note>
      <@render_note note />
    </#list>
  </div>
</#escape>