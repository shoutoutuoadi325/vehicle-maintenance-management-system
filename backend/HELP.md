# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.5/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.4.5/maven-plugin/build-image.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.4.5/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Validation](https://docs.spring.io/spring-boot/3.4.5/reference/io/validation.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.5/reference/web/servlet.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

### Local Environment Quick Check (Windows)

Use the helper script before running backend tests:

```powershell
Set-Location .\backend
.\scripts\verify-dev-env.ps1
```

Run full backend tests after the check:

```powershell
Set-Location .\backend
.\scripts\verify-dev-env.ps1 -RunTests
```

### Local Quality Gate (Windows)

Run compile + full tests in one command:

```powershell
Set-Location .\backend
.\scripts\quality-gate.ps1
```

Optional modes:

```powershell
# Skip environment precheck (when already verified)
.\scripts\quality-gate.ps1 -SkipEnvCheck

# Compile-only gate (fast path)
.\scripts\quality-gate.ps1 -SkipTests
```

### Standardized Release Gate (Windows)

Run release gate with quality checks, migration validation and post-release smoke:

```powershell
Set-Location .\backend
.\scripts\release-gate.ps1 -BaseUrl http://localhost:8080
```

Optional mode:

```powershell
# Skip smoke during pre-release dry run
.\scripts\release-gate.ps1 -SkipSmoke
```

### DB Migration Validation

```powershell
Set-Location .\backend
.\scripts\validate-migrations.ps1
```

### Operability SOP and Debt Ledger

- Operability runbook: `OPERABILITY_RUNBOOK.md`
- Architecture debt ledger: `ARCHITECTURE_DEBT_LEDGER.md`
- Gray release template: `GRAY_RELEASE_PLAYBOOK.md`

