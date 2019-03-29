Feature: Standard server headers
  As a security-conscious service owner
  I want to expose as little information about my server implementation as possible

  Scenario Outline: Requesting a page, no gzip header (Vary)
    When I request <url>
    Then the Vary response header should be "Accept-Encoding"

    Examples:
      | url                        |
      | https://@HOST@/            |
      | https://@HOST@/coursework/ |

  Scenario: Request file from static
    Given there is a file at https://@HOST@/static/js/id7/home.js
    When I request https://@HOST@/static/js/id7/home.js
    And I follow redirects
    Then the status code should be 200
    And the Vary response header should be "Accept-Encoding"
    And the Content-Type response header should be "application/javascript"
    And the ETag response header should exist
    And the Cache-Control response header should be "public, max-age=31536000, immutable"