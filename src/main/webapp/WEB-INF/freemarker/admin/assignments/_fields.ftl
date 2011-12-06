<#macro datefield path label>
<@form.labelled_row path label>
<@f.input path=path cssClass="date-time-picker" />
</@form.labelled_row>
</#macro>

<@form.labelled_row "name" "Assignment name">
<@f.input path="name" />
</@form.labelled_row>

<@datefield path="openDate" label="Open date" />
<@datefield path="closeDate" label="Close date" />