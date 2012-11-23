<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="Coursework submission">
    <Require feature="dynamic-height" />
    <OAuth>
      <Service name="courses">
        <Access url="https://websignon.warwick.ac.uk/oauth/accessToken" method="GET" />
        <Request url="https://websignon.warwick.ac.uk/oauth/requestToken?scope=${oauthScope?url}" method="GET" />
        <Authorization url="https://websignon.warwick.ac.uk/oauth/authorise" />
      </Service>
    </OAuth>
  </ModulePrefs>
  <Content type="html">
  	 <![CDATA[
       Hello, courses!

       <script type="text/javascript">
       var prefs = new gadgets.Prefs();
       gadgets.util.registerOnLoadHandler(function(){



       });
       </script>
     ]]>
  </Content>
</Module>