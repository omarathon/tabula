<title><#if component.subsite>Tabula - </#if>${component.title?default('Tabula')}</title>

<meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1" >
<meta charset="utf-8">
<meta name="robots" content="noindex,nofollow">

<meta name="HandheldFriendly" content="True">
<meta id="meta-mobile-optimized" name="MobileOptimized" content="320">
<meta id="meta-viewport" name="viewport" content="width=device-width">
<meta http-equiv="cleartype" content="on">

<@stylesheet "/static/css/concat6.css" />
<#include "_styles.ftl" />
<link rel="stylesheet" title="No Accessibility" href="<@url resource="/static/css/noaccessibility.css"/>" type="text/css">
<link rel="alternate stylesheet" title="Show Accessibility" href="<@url resource="/static/css/showaccessibility.css"/>" type="text/css">

<!--[if lt IE 8]>
<@stylesheet "/static/css/ielt8.css" />
<![endif]-->
<!--[if lt IE 9]>
<style type="text/css">
	#container {
	behavior: url(/static/css/pie.htc);
	}
</style>
<![endif]-->

<@stylesheet "/static/libs/popup/popup.css" />
<@stylesheet "/static/libs/jquery-rating/jquery.rating.css" />

<#include "_scripts.ftl" />

<@script "/static/libs/jquery-ui/js/jquery-ui-1.8.16.custom.min.js" />
<@script "/static/libs/jquery.delayedObserver.js" />
<@script "/static/libs/jquery-rating/jquery.rating.pack.js" />
<@script "/static/libs/anytime/anytimec.js" />
<@script "/static/libs/popup/popup.js" />
<@script "/static/libs/bootstrap/js/bootstrap.js" />
<@script "/static/libs/bootstrap-editable/js/bootstrap-editable.js" />
<@script "/static/js/modernizr.js" />
<@script "/static/js/browser-info.js" />
<@script "/static/js/${component.name?default('common')}.js" />