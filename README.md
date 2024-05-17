# Description

Accessibility scanner library. This is a wrapper for the selenium axe framework.
Once the scan has completed and any issues found, there will be a REPORT folder created in the project 
that consumes this library.

# Prerequisites
- Maven

# Rules

This library uses AXE-CORE to scan for accessibility violations. The full rules that the scan covers
can be found on the following link https://dequeuniversity.com/rules/axe/3.2

## Installation
Add the following Maven dependency to your project's `pom.xml` file:
```xml
<dependency>
    <groupId>org.dvsa.testing.framework</groupId>
    <artifactId>accessibility-library</artifactId>
    <version>[insert latest version of package]</version>
</dependency>
```

