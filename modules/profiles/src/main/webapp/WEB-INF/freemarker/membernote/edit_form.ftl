<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<@f.form id="edit-member-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">
<@form.labelled_row "title" "Title">
<@f.input type="text" path="title" cssClass="input-block-level" maxlength="255" placeholder="The title" />
</@form.labelled_row>

<@form.labelled_row "note" "Note">
<@f.textarea path="note" cssClass="input-block-level" rows="5" maxlength="255" />
</@form.labelled_row>
</@f.form>