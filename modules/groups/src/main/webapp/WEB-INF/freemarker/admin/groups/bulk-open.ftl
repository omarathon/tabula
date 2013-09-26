<#escape x as x?html>

 <#if setState == 'close'>
 	<#assign pastTense = "d" />
 	<#assign stateChange = false />
 <#else>
 	<#assign pastTense = "" />
 	<#assign stateChange = true />
 </#if>


<h1>${setState?cap_first} groups in ${department.name}</h1>
<#if info.requestParameters.batchOpenSuccess?? && setState == "open">
<div class="alert alert-success">
    Students have been notified that these groups are now ${setState} for self-sign-up
</div>
</#if>
    <@f.form method="post" action="" commandName="setList" cssClass="form-horizonatal form-tiny">
    <p> ${setState?cap_first} these groups for self sign-up.
    <#if setState == "open"> Students will be notified via email that they can now sign up for these groups in Tabula. </#if>
    </p>
        <div class="control-group">
            <input class="btn btn-info" type="submit" value="${setState?cap_first}">
        </div>

    <div id="scroll-container">
    <table id="groups-table" class="table table-bordered table-striped sortable-table">

        <thead>
        <tr>
            <th><@form.selector_check_all /></th>
            <th class="sortable">Module/Group</th>
        </tr>
        </thead>
        <tbody >
            <#list groupSets as set>
            <tr ${(set.openForSignups == stateChange)?string("class='use-tooltip' title='This group is already ${setState}${pastTense} for sign-ups'","")} >
                <td>
                   <@f.checkbox
                   class=(set.openForSignups == stateChange)?string('','collection-checkbox')
                   path="checkedGroupsets"
                   disabled=(set.openForSignups == stateChange)?string
                   value=set.id/>
                </td>
                <td>
                    <span class=${(set.openForSignups == stateChange)?string("muted","")}>
                    	<@fmt.module_name set.module false /> - ${set.name}
                    </span>
                </td>
            </tr>
            </#list>

        </tbody>
    </table>
    </div>


    </@f.form>
</#escape>
<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="/static/js/sortable-table.js"></script>
<script type="text/javascript">
    jQuery("#groups-table").bigList({});
</script>