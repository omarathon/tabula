<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign time_remaining=durationFormatter(assignment.closeDate) />

<div>
    <h1>Authorise late submissions for ${assignment.name}</h1>
    <p>
       This assignment closes on <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>.
       To authorise an extension for a student click the "Authorise" button next to the students university ID.
    </p>
    <@f.form id="extension-list" method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/extensions" commandName="extensionCommand">
        <table class="extensionListTable">
            <tr class="extension-header"><th>University ID</th><th>New submission date</th></tr>
        </table>
        <br /><button class="add-extensions btn"><i class="icon-plus"></i> Add</button>
        <div class="submit-buttons">
            <input type="submit" class="btn btn-primary" value="Save">
            or <a href="<@routes.depthome module=assignment.module />" class="btn">Cancel</a>
        </div>
    </@f.form>
</div>

</#escape>