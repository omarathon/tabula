<#assign maxFiles=field.attachmentLimit />
<@form.filewidget basename="fields[${field.id}].file" multiple=(maxFiles gt 1) max=maxFiles />
