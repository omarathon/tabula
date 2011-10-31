<#macro datefield path label>
<div>
<@f.label path=path>
<@f.errors path=path cssClass="error" />
${label}
</@f.label>
<@f.input path=path cssClass="date-time-picker" />
</div>
</#macro>

<div>
<@f.label path="name">
<@f.errors path="name" cssClass="error" />
Assignment name
</@f.label>
<@f.input path="name" />
</div>

<@datefield path="openDate" label="Open date" />
<@datefield path="closeDate" label="Close date" />