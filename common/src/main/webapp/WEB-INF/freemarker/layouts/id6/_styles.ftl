<#escape x as x?html>
<#-- Styles that should be included both in the app and embedded in to Sitebuilder -->
<@stylesheet "/static/css/bootstrap.css" />
<@stylesheet "/static/css/render.css" />

<link href='//fonts.googleapis.com/css?family=Bitter:400,700,400italic&subset=latin,latin-ext' rel='stylesheet' type='text/css' crossorigin='anonymous'>

<link rel="stylesheet" title="No Accessibility" href="<@url resource="/static/css/noaccessibility.css"/>" type="text/css">
<link rel="alternate stylesheet" title="Show Accessibility" href="<@url resource="/static/css/showaccessibility.css"/>" type="text/css">

<!--[if lt IE 8]>
<@stylesheet "/static/css/ielt8.css" />
<![endif]-->
<!--[if lt IE 9]>
<style type="text/css" nonce="${nonce()}">
  #container {
    behavior: url(/static/css/pie.htc);
  }
</style>
<![endif]-->
</#escape>