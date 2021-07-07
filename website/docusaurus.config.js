module.exports={
  "title": "Pitchfork",
  "tagline": "Convert tracing data between Zipkin and Haystack formats",
  "url": "https://expediagroup.github.io",
  "baseUrl": "/pitchfork/",
  "organizationName": "ExpediaGroup",
  "projectName": "Pitchfork",
  "scripts": [
    "https://buttons.github.io/buttons.js"
  ],
  "favicon": "img/pitchfork_devil.svg",
  "customFields": {
    "users": [
      {
        "caption": "Hotels.com",
        "image": "img/hotels_logo.svg",
        "infoLink": "https://www.hotels.com",
        "pinned": true
      },
      {
        "caption": "Expedia Group",
        "image": "img/expedia_group_logo.png",
        "infoLink": "https://www.expediagroup.com/",
        "pinned": true
      }
    ],
    "repoUrl": "https://github.com/ExpediaGroup/pitchfork"
  },
  "onBrokenLinks": "log",
  "onBrokenMarkdownLinks": "log",
  "presets": [
    [
      "@docusaurus/preset-classic",
      {
        "docs": {
          "homePageId": "about/introduction",
          "showLastUpdateAuthor": true,
          "showLastUpdateTime": true,
          "path": "../docs",
          "sidebarPath": "../website/sidebars.json"
        },
        "blog": {},
        "theme": {
          "customCss": "../src/css/customTheme.css"
        }
      }
    ]
  ],
  "plugins": [],
  "themeConfig": {
    "navbar": {
      "title": "Pitchfork",
      "logo": {
        "src": "img/pitchfork_devil.svg"
      },
      "items": [
        {
          "to": "docs/",
          "label": "Docs",
          "position": "left"
        },
        {
          "href": "https://github.com/ExpediaGroup/pitchfork",
          "label": "GitHub",
          "position": "left"
        }
      ]
    },
    "image": "img/pitchfork_logo.svg",
    "footer": {
      "links": [],
      "copyright": "Copyright © 2021 Expedia, Inc.",
      "logo": {
        "src": "img/pitchfork_devil.svg"
      }
    }
  }
}