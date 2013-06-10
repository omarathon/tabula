<#assign maxFiles=1 />
<#assign fileTypes=".xls" />
<@form.filewidget basename="allocate" types=fileTypes multiple=(maxFiles gt 1) max=maxFiles />
