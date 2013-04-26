Feature: GZipped content
  In order to minimise download times
  Suitable mime-types should be GZipped before serving
  
  Scenario Outline: Request GZipped script in static from various browsers
    Given there is a file at https://HOST/static/js/home.js.484115321229 
    When I request https://HOST/static/js/home.js.484115321229
    And I set the header accept-encoding to "gzip"
    And I set the header user-agent to "<user_agent>"
    Then the content-encoding response header <expected_encoding>
    
  Examples:
    | user_agent                                                                                | expected_encoding |
    | Mozilla/5.0 (Windows; U; Windows NT 6.1; pt-PT; rv:1.9.2.6) Gecko/20100625 Firefox/3.6.6  | should be "gzip"  |
    | Mozilla/4.0 (Windows; MSIE 6.0; Windows NT 6.0)                                           | should not exist  |
    | Mozilla/5.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4325)                | should be "gzip"  |