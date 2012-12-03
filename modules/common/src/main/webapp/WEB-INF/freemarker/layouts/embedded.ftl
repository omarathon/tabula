<#assign tiles=JspTaglibs["http://tiles.apache.org/tags-tiles"]>
<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
<!doctype html>
<html lang="en-GB">
	<head>		  
		  <title><#if component.subsite>Tabula - </#if>${component.title?default('Tabula')}</title>
		
		  <meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1" >
		  <meta charset="utf-8">
		  <meta name="robots" content="noindex,nofollow">

		  <link rel="stylesheet" href="/static/css/concat6.css" type="text/css">
		  <#include "_styles.ftl" />
		  <link rel="stylesheet" title="No Accessibility" href="/static/css/noaccessibility.css" type="text/css">
		  <link rel="alternate stylesheet" title="Show Accessibility" href="/static/css/showaccessibility.css" type="text/css">
		  
		  <!--[if lt IE 8]>
			  <link rel="stylesheet" href="/static/css/ielt8.css" type="text/css">
		  <![endif]-->
		  <!--[if lt IE 9]>
		  	<style type="text/css">
		 		#container {
					behavior: url(/static/css/pie.htc);
		 		}
		  	</style>
		  <![endif]-->
		  	
		  <#include "_scripts.ftl" />

	</head>
	<body>
		<div class="tabula-page ${component.bodyClass?default('component-page')">
		<@tiles.insertAttribute name="body" />
		</div>
	</body>
</html>