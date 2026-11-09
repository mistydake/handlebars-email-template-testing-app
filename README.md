# Handlebars Email Template Testing App

A Spring Boot application for testing Handlebars email templates built with jknack's Handlebars.java port of Handlebars.js, so they can be checked in a Java application environment, with several included custom helpers.

A small Spring Boot web app for editing and rendering Handlebars email templates locally,
using the [jknack Handlebars.java](https://github.com/jknack/handlebars.java) engine (4.2.0).

Edit a template, a JSON payload and custom JavaScript helpers in the browser, then render
the result server-side and see it instantly, with line and column numbers when something is wrong.

---

## Features

- Three editors (Template HTML, Payload JSON, Helpers JS) with syntax highlighting and 4-space tabs
- Inline preview, plus "Open in New Tab"
- Built-in **sample template, payload and helpers**, loaded on first launch (or any time with **Load Sample**)
- Payload sanitizing before every render:
  - comments (`//`, `/* */`) and trailing commas are removed
  - the result is validated as a JSON object, with the error line and column shown if it is not valid
  - strings that contain a JSON array or object (e.g. `"items": "[{...}]"`) are expanded into real data
  - the cleaned payload is shown in the editor, with **Restore Original** to bring your text back
- Collapsible error panel for template and payload errors
- Save Files writes your three editors to disk next to the JAR

---

## Requirements

- Java 11 or higher (`java -version`). The JavaScript engine used for helpers (Nashorn) is bundled, so newer Java versions such as 17 and 21 work too
- Maven 3.6+ to build (`mvn -version`), or let GitHub Actions build the JAR for you

## Build

**Windows:** `build.bat`  
**Mac / Linux:** `chmod +x build.sh && ./build.sh`

This creates `target/handlebars-email-template-testing-app-1.0.jar`.

Using GitHub instead: push to `main`, open **Actions → Build JAR**, and download the
`handlebars-email-template-testing-app-jar` artifact from the finished run.

## Run

1. Put the JAR in any folder.
2. Open a terminal in that folder and run:

```
java -jar handlebars-email-template-testing-app-1.0.jar
```

The browser opens at `http://localhost:8080`. The first launch shows the built-in sample.

If you click **Save Files**, three files are created beside the JAR and loaded next time:

- `template.html`
- `payload.json`
- `customhandlebar.js`

To use a different port: `java -jar handlebars-email-template-testing-app-1.0.jar --server.port=9090`

---

## The sample

| File | What it shows |
|------|---------------|
| `template.html` | An order receipt using `{{#each}}`, `{{#with}}`, `{{#if}}`, `../parent` access, subexpressions like `(multiply price quantity)`, and a custom block helper |
| `payload.json` | A flat JSON payload with comments, a trailing comma, and `items` / `notes` stored as stringified JSON |
| `customhandlebar.js` | Custom helpers: `formatCurrency`, `multiply`, `sumLineTotals`, `taxAmount`, `orderTotal`, `percent`, `count`, `pluralize`, `ifEquals` |

Built-in Java helpers that are always available: `dateFormat`, `lower`, `upper`, plus jknack's
string and conditional helpers.

The source of the samples is in `src/main/resources/samples/`.

---

## Notes

- Custom helpers run on the JDK's JavaScript engine through Handlebars.java. Register them with
  `Handlebars.registerHelper('name', function (arg1, arg2, options) { ... })`. The last argument is
  always the options object (`options.fn(this)`, `options.inverse(this)`, `options.hash("key")`).
- To stop the server, press `Ctrl+C` in the terminal.
