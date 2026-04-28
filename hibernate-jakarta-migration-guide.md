---
title: Hibernate 6 & Jakarta Migration Guide for Modules
tags:
    - Hibernate 6
    - Jakarta EE
    - Module Migration
    - DAL
    - Restrictions
    - API Changes
---

# Hibernate 6 & Jakarta Migration Guide for Modules

## Overview

Starting from **Etendo 27**, the platform has been upgraded from **Hibernate 5.x** to **Hibernate 6.x** and from **Java EE (javax.\*)** to **Jakarta EE (jakarta.\*)**. While the Etendo Core has already been migrated, custom modules must be updated to compile and run correctly against the new stack.

This guide documents each breaking change and provides the exact replacement to apply in your module's code.

!!! info "Apply changes incrementally"
    Each section below is independent. Work through them one by one and recompile after each change to catch errors early.

---

## Changes

### 1. `org.hibernate.criterion.Restrictions` → `org.openbravo.dal.service.Restrictions`

The legacy Hibernate Criteria API (`org.hibernate.criterion.Restrictions`) was removed in Hibernate 6. Etendo provides a drop-in replacement at `org.openbravo.dal.service.Restrictions`, backed internally by the JPA Criteria API (`jakarta.persistence.criteria`).

#### How to migrate

Replace the import in every file that uses `Restrictions`:

```java title="Before (Hibernate 5.x)"
import org.hibernate.criterion.Restrictions;
```

```java title="After (Hibernate 6.x)"
import org.openbravo.dal.service.Restrictions;
```

No changes to call sites are required. All methods retain the same signatures:

| Method | Description |
|---|---|
| `Restrictions.eq(property, value)` | Equal |
| `Restrictions.ne(property, value)` | Not equal |
| `Restrictions.gt(property, value)` | Greater than |
| `Restrictions.ge(property, value)` | Greater than or equal |
| `Restrictions.lt(property, value)` | Less than |
| `Restrictions.le(property, value)` | Less than or equal |
| `Restrictions.like(property, value)` | LIKE |
| `Restrictions.ilike(property, value)` | Case-insensitive LIKE |
| `Restrictions.in(property, values...)` | IN (varargs or Collection) |
| `Restrictions.isNull(property)` | IS NULL |
| `Restrictions.isNotNull(property)` | IS NOT NULL |
| `Restrictions.between(property, low, high)` | BETWEEN |
| `Restrictions.eqProperty(prop1, prop2)` | Equal (two properties) |
| `Restrictions.idEq(value)` | Equal on identifier |
| `Restrictions.allEq(Map)` | Equal on all map entries |
| `Restrictions.and(lhs, rhs)` | Logical AND |
| `Restrictions.or(lhs, rhs)` | Logical OR |
| `Restrictions.not(restriction)` | Logical NOT |
| `Restrictions.conjunction()` | Empty AND group (buildable) |
| `Restrictions.disjunction()` | Empty OR group (buildable) |
| `Restrictions.eqOrIsNull(property, value)` | Equal or IS NULL if value is null |
| `Restrictions.neOrIsNotNull(property, value)` | Not equal or IS NOT NULL if value is null |

!!! warning "Return type is `Restriction`, not `Criterion`"
    The new class returns `org.openbravo.dal.service.Restriction` instances instead of `org.hibernate.criterion.Criterion`. Update any typed variable declarations:

    ```java title="Before"
    Criterion c = Restrictions.eq("active", true);
    ```

    ```java title="After"
    Restriction c = Restrictions.eq("active", true);
    ```

    If you pass the result directly to an `OBCriteria` method without storing it, no change is needed at the call site.

!!! warning "Inline fully-qualified calls"
    If your code uses the fully-qualified form `org.hibernate.criterion.Restrictions.eq(...)` inline (without an import), replace the qualifier too:

    ```java title="Before"
    .add(org.hibernate.criterion.Restrictions.eq("javaClassName", actionClassName))
    ```

    ```java title="After"
    .add(Restrictions.eq("javaClassName", actionClassName))
    ```

---

### 2. `javax.enterprise.*` and `javax.inject.*` → `jakarta.*`

Jakarta EE 9 renamed the base package from `javax` to `jakarta`. The CDI and injection annotations are the most commonly used ones affected.

#### How to migrate

Replace the following imports wherever they appear:

| Before (Java EE / javax) | After (Jakarta EE) |
|---|---|
| `import javax.enterprise.context.ApplicationScoped;` | `import jakarta.enterprise.context.ApplicationScoped;` |
| `import javax.enterprise.context.Dependent;` | `import jakarta.enterprise.context.Dependent;` |
| `import javax.enterprise.event.Observes;` | `import jakarta.enterprise.event.Observes;` |
| `import javax.enterprise.inject.Any;` | `import jakarta.enterprise.inject.Any;` |
| `import javax.enterprise.inject.Instance;` | `import jakarta.enterprise.inject.Instance;` |
| `import javax.enterprise.inject.Vetoed;` | `import jakarta.enterprise.inject.Vetoed;` |
| `import javax.enterprise.inject.spi.Bean;` | `import jakarta.enterprise.inject.spi.Bean;` |
| `import javax.enterprise.inject.spi.BeanManager;` | `import jakarta.enterprise.inject.spi.BeanManager;` |
| `import javax.enterprise.util.AnnotationLiteral;` | `import jakarta.enterprise.util.AnnotationLiteral;` |
| `import javax.inject.Inject;` | `import jakarta.inject.Inject;` |

The annotation names and semantics are identical — only the package prefix changes.

!!! info "`javax.servlet.*` and JDK javax packages"
    `javax.servlet.*` also moves to `jakarta.servlet.*` in Jakarta EE 9. This applies to both import statements **and** inline fully-qualified names in method signatures and code bodies (e.g., `extends javax.servlet.http.HttpServletResponseWrapper`). However, `javax.net.ssl.*`, `javax.script.*`, and `javax.mail.*` are JDK or JavaMail packages that remain under `javax` and should **not** be changed.

---

### 3. `org.hibernate.criterion.Order` / `OBCriteria.addOrder()` → `OBCriteria.addOrderBy()`

The `org.hibernate.criterion.Order` class and the `OBCriteria.addOrder(Order)` method were removed. `OBCriteria` now exposes `addOrderBy(String property, boolean ascending)`.

#### How to migrate

Remove the `Order` import and replace each `addOrder` call:

```java title="Before"
import org.hibernate.criterion.Order;

criteria.addOrder(Order.asc(MyEntity.PROPERTY_NAME));
criteria.addOrder(Order.desc(MyEntity.PROPERTY_DATE));
```

```java title="After"
// no import needed

criteria.addOrderBy(MyEntity.PROPERTY_NAME, true);   // true = ascending
criteria.addOrderBy(MyEntity.PROPERTY_DATE, false);  // false = descending
```

!!! info "In Mockito tests"
    Tests that verified `addOrder` calls must be updated to verify `addOrderBy`:

    ```java title="Before"
    verify(criteria, atLeastOnce()).addOrder(any(Order.class));
    ```

    ```java title="After"
    verify(criteria, atLeastOnce()).addOrderBy(any(String.class), anyBoolean());
    ```

---

### 4. `org.hibernate.criterion.MatchMode` → removed

`MatchMode` was part of the legacy Hibernate Criteria API and has no equivalent in the new stack. The `Restrictions.ilike()` replacement accepts a plain string pattern.

#### How to migrate

| Old usage | Equivalent new usage |
|---|---|
| `Restrictions.ilike(prop, value, MatchMode.ANYWHERE)` | `Restrictions.ilike(prop, "%" + value + "%")` |
| `Restrictions.ilike(prop, value, MatchMode.START)` | `Restrictions.ilike(prop, value + "%")` |
| `Restrictions.ilike(prop, value, MatchMode.END)` | `Restrictions.ilike(prop, "%" + value)` |
| `Restrictions.ilike(prop, value, MatchMode.EXACT)` | `Restrictions.ilike(prop, value)` |

Remove the `import org.hibernate.criterion.MatchMode;` line.

---

### 5. `Session.createSQLQuery()` → `Session.createNativeQuery()`

`Session.createSQLQuery(String sql)` was removed in Hibernate 6. The replacement is `createNativeQuery(String sql, Class<T> resultType)`, which requires an explicit result type.

#### How to migrate

```java title="Before (Hibernate 5.x)"
String result = (String) session.createSQLQuery("select get_uuid()")
    .setMaxResults(1)
    .uniqueResult();
```

```java title="After (Hibernate 6.x)"
String result = session.createNativeQuery("select get_uuid()", String.class)
    .setMaxResults(1)
    .uniqueResult();
```

!!! info "Via OBDal"
    When calling through `OBDal.getInstance().getSession()`, the same replacement applies:

    ```java title="Before"
    OBDal.getInstance().getSession().createSQLQuery(sql).setMaxResults(1).uniqueResult();
    ```

    ```java title="After"
    OBDal.getInstance().getSession().createNativeQuery(sql, MyResultType.class).setMaxResults(1).uniqueResult();
    ```

---

---

### 6. `HttpSessionContext` removed

`jakarta.servlet.http.HttpSessionContext` and the `HttpSession.getSessionContext()` method were removed in Jakarta Servlet 6.0 (they had been deprecated since Servlet 2.1). The class no longer exists in the API.

#### How to migrate

Remove the import and delete the `getSessionContext()` override entirely. It was always a no-op:

```java title="Before"
import jakarta.servlet.http.HttpSessionContext;

@Override
@Deprecated
public HttpSessionContext getSessionContext() {
    return null;
}
```

```java title="After"
// import removed
// method removed
```

---

### 7. `Restriction.ignoreCase()` removed — use `Restrictions.ilike()`

The legacy `ignoreCase()` method on `Criterion` (now `Restriction`) no longer exists. For case-insensitive equality, use `Restrictions.ilike()` with the exact value.

#### How to migrate

```java title="Before"
criteria.add(Restrictions.eq(MyEntity.PROPERTY_NAME, value).ignoreCase());
```

```java title="After"
criteria.add(Restrictions.ilike(MyEntity.PROPERTY_NAME, value));
```

`Restrictions.ilike()` performs a case-insensitive match (equivalent to PostgreSQL `ILIKE`). When you need an exact match regardless of case, pass the value without wildcards.

---

### 8. `ScrollableResults.get()` return type changed

In Hibernate 5, `ScrollableResults.get()` returned `Object[]` (an array), so the first column was accessed as `get()[0]`. In Hibernate 6, `ScrollableResults<R>` is generic and `get()` returns `R` directly.

#### How to migrate

```java title="Before (Hibernate 5.x)"
OBCriteria<MyEntity> criteria = OBDal.getInstance().createCriteria(MyEntity.class);
try (ScrollableResults scroller = criteria.scroll()) {
    while (scroller.next()) {
        MyEntity entity = (MyEntity) scroller.get()[0];
    }
}
```

```java title="After (Hibernate 6.x)"
OBCriteria<MyEntity> criteria = OBDal.getInstance().createCriteria(MyEntity.class);
try (ScrollableResults<MyEntity> scroller = criteria.scroll()) {
    while (scroller.next()) {
        MyEntity entity = scroller.get();
    }
}
```

Remove the `[0]` array access. The cast is also unnecessary when the generic type is specified.

---

### 9. `OBCriteria.setFetchSize()` removed

`OBCriteria.setFetchSize(int)` no longer exists. It was a Hibernate Criteria hint that had no JPA equivalent in the migrated implementation.

#### How to migrate

Remove the call entirely — it was a performance hint and the query will still execute correctly without it:

```java title="Before"
OBCriteria<MyEntity> criteria = OBDal.getInstance().createCriteria(MyEntity.class);
criteria.setFetchSize(1000);
```

```java title="After"
OBCriteria<MyEntity> criteria = OBDal.getInstance().createCriteria(MyEntity.class);
// setFetchSize removed — no replacement needed
```

---

### 10. Jakarta Servlet 6.0: removed and added interface methods

Jakarta Servlet 6.0 removed two long-deprecated methods from the servlet interfaces and added one new abstract method. Custom implementations of `HttpServletRequest` or `ServletRequest` must be updated.

#### Removed methods

| Interface | Method removed |
|---|---|
| `HttpServletRequest` | `isRequestedSessionIdFromUrl()` |
| `ServletRequest` | `getRealPath(String)` |

Remove the `@Override` annotation from these methods (or delete them). Keeping the implementations without `@Override` is safe and preserves backwards compatibility for any callers.

```java title="Before"
@Override
public boolean isRequestedSessionIdFromUrl() { return false; }

@Override
public String getRealPath(String path) { return null; }
```

```java title="After"
public boolean isRequestedSessionIdFromUrl() { return false; }

public String getRealPath(String path) { return null; }
```

#### Added methods

Two abstract methods were added to `ServletRequest` in Jakarta Servlet 6.0. Implement them to return safe defaults in stub/dummy implementations:

```java title="After"
@Override
public String getRequestId() {
    return "";
}

@Override
public String getProtocolRequestId() {
    return "";
}

@Override
public jakarta.servlet.ServletConnection getServletConnection() {
    return null;
}
```

---

### 11. `Property.setValueGenerationStrategy()` → `Property.setValueGeneratorCreator()`

`org.hibernate.mapping.Property.setValueGenerationStrategy(AnnotationValueGeneration)` was removed in Hibernate 6. The replacement is `setValueGeneratorCreator(GeneratorCreator)`.

`AnnotationValueGeneration` extends `Generator` in Hibernate 6, so an existing `DefaultSequenceGenerator` instance can be passed using a lambda that ignores the context argument.

#### How to migrate

```java title="Before (Hibernate 5.x)"
import org.hibernate.mapping.Property;

entity.getProperty(propertyName).setValueGenerationStrategy(generator);
```

```java title="After (Hibernate 6.x)"
// no new import needed — GeneratorCreator is a functional interface

final DefaultSequenceGenerator finalGenerator = generator;
entity.getProperty(propertyName).setValueGeneratorCreator(context -> finalGenerator);
```

The variable captured by the lambda must be effectively final. If `generator` is reassigned in a loop, introduce a `final` local copy (`finalGenerator`) before the lambda.

This change typically appears in auto-generated `*SequenceContributor` classes under `src-gen/`. The fix must also be applied to the FreeMarker template at `src/org/openbravo/base/gen/entitySequenceContributor.ftl` so that newly generated files use the correct API.

---

### 12. `Restrictions.sqlRestriction()` — native SQL predicates

The legacy `Restrictions.sqlRestriction(String sql)` method is available in the Etendo `org.openbravo.dal.service.Restrictions` class. Use `{alias}` as a placeholder for the entity's table alias (resolved at query time to `"e"` by default).

#### Usage

```java
criteria.add(Restrictions.sqlRestriction(
    "exists (select 1 from ad_user_roles ur where ur.ad_user_id = {alias}.ad_user_id)"));
```

!!! warning "Use sparingly"
    `sqlRestriction` embeds raw SQL and bypasses the JPA type system. Prefer typed `Restrictions` methods or JPA Criteria subqueries whenever possible.

---

### 13. `org.hibernate.criterion.Criterion` in Mockito tests

The `org.hibernate.criterion.Criterion` interface was removed in Hibernate 6. Test files that used it as a type argument in Mockito matchers or `ArgumentCaptor` fail to compile because the class no longer exists on the classpath.

#### How to migrate

Replace all occurrences of `Criterion` with `Restriction` (from `org.openbravo.dal.service`). The `Restriction` import is typically already present if the production code under test was already migrated (see [Change 1](#1-orghibernatecriterionrestrictions--orgopenbravodalservicerestrictions)).

```java title="Before (Hibernate 5.x)"
import org.hibernate.criterion.Criterion; // often missing — was resolved transitively

when(criteria.add(any(Criterion.class))).thenReturn(criteria);

ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
List<Criterion> captured = captor.getAllValues();
```

```java title="After (Hibernate 6.x)"
import org.openbravo.dal.service.Restriction; // already present after Change 1

when(criteria.add(any(Restriction.class))).thenReturn(criteria);

ArgumentCaptor<Restriction> captor = ArgumentCaptor.forClass(Restriction.class);
List<Restriction> captured = captor.getAllValues();
```

The same applies to `verify()` calls:

```java title="Before"
verify(criteria, times(2)).add(any(Criterion.class));
```

```java title="After"
verify(criteria, times(2)).add(any(Restriction.class));
```

!!! info "No import to remove"
    Most affected files never had an explicit `import org.hibernate.criterion.Criterion;` — the class was resolved transitively from `hibernate-core`. After removing that dependency the symbol disappears. Simply add (or confirm) the `import org.openbravo.dal.service.Restriction;` line.

---

### 13b. `org.hibernate.Criteria` unused import in test files

`org.hibernate.Criteria` (the legacy criteria interface) was removed in Hibernate 6. Test files that carry a leftover `import org.hibernate.Criteria;` fail to compile even when the symbol is never referenced in the code body — the import line alone is enough to break the build.

#### How to migrate

Simply remove the import. The actual criteria work goes through `OBCriteria` (Etendo's JPA-backed wrapper) — no replacement needed:

```java title="Before"
import org.hibernate.Criteria; // unused — but still causes compilation failure
import org.openbravo.dal.service.OBCriteria;
```

```java title="After"
// import org.hibernate.Criteria; removed
import org.openbravo.dal.service.OBCriteria;
```

!!! tip "Quick scan"
    Run `grep -r "import org.hibernate.Criteria" modules/` to find all affected test files in one pass.

---

### 14. Jakarta Servlet 6.0: test methods calling removed interface methods

When test files call `isRequestedSessionIdFromUrl()`, `getRealPath(String)`, or `getSessionContext()` on a variable typed as the servlet interface (`HttpServletRequest`, `ServletRequest`, or `HttpSession`), they fail to compile because those methods were removed from the interface in Jakarta Servlet 6.0.

#### How to migrate

**`isRequestedSessionIdFromUrl()` and `getRealPath(String)` on `HttpServletRequest` / `ServletRequest`**

Remove the assertions entirely — the methods no longer exist on the interface:

```java title="Before"
assertFalse(req.isRequestedSessionIdFromUrl());
// ...
assertNull(req.getRealPath("/any"));
```

```java title="After"
// both lines removed — isRequestedSessionIdFromUrl() and getRealPath() no
// longer exist on the HttpServletRequest / ServletRequest interface
```

**`getSessionContext()` on `HttpSession`**

Remove test methods that verify `getSessionContext()`. The method was removed from `HttpSession` in Jakarta Servlet 6.0 and from `LegacyHttpSessionAdapter` as part of Change 6:

```java title="Before"
@Test
public void getSessionContextShouldReturnNull() {
    assertNull(session.getSessionContext());
}

@Test(expected = IllegalStateException.class)
public void getSessionContextAfterInvalidateShouldThrowException() {
    session.invalidate();
    session.getSessionContext();
}
```

```java title="After"
// both test methods removed — getSessionContext() no longer exists
```

---

### 15. Kafka `Admin.describeTopics()` ambiguous overload in Mockito

Kafka 3.x added a second overload `describeTopics(TopicCollection)` alongside the existing `describeTopics(Collection<String>)`. Mockito's untyped `any()` matcher becomes ambiguous when both overloads are present, causing a compilation error.

#### How to migrate

Qualify the matcher with the concrete type to disambiguate:

```java title="Before"
when(mockAdminClient.describeTopics(any())).thenReturn(...);
when(mockAdminClient.describeTopics(any()).all()).thenReturn(...);
```

```java title="After"
when(mockAdminClient.describeTopics(any(java.util.Collection.class))).thenReturn(...);
when(mockAdminClient.describeTopics(any(java.util.Collection.class)).all()).thenReturn(...);
```

No new import is needed — `java.util.Collection` can be used as a fully-qualified name inline.

---

### 16. Integration tests: HTTP test utilities crashing on non-JSON responses

Integration tests that make real HTTP calls (e.g. to `/webhooks/*`) use utility methods that read the response body and parse it as JSON. After the migration, certain error responses may be returned as HTML (Tomcat error pages or authentication-filter redirects) instead of JSON. When the utility method tries to parse HTML as JSON, Jackson throws `JsonParseException: Unexpected character ('<' (code 60))`, and the test fails with an unrelated-looking error that hides the real HTTP status code.

#### How to migrate

Wrap the JSON parse in a `try/catch` so that non-JSON responses produce a meaningful failure message:

```java title="Before — crashes with JsonParseException on HTML responses"
JsonNode jsonNode = objectMapper.readTree(errorContent.toString());
return new WebhookHttpResponse(responseCode, jsonNode.get("message").asText());
```

```java title="After — fails with actionable message on non-JSON body"
String errorBody = errorContent.toString();
try {
    JsonNode jsonNode = objectMapper.readTree(errorBody);
    return new WebhookHttpResponse(responseCode, jsonNode.get("message").asText());
} catch (Exception jsonEx) {
    // Server returned non-JSON (e.g. HTML error page from authentication filter).
    log4j.error("Non-JSON error response (HTTP " + responseCode + "): " + errorBody);
    fail("Expected JSON from endpoint but got HTTP " + responseCode +
        ". Response (first 500 chars): " +
        errorBody.substring(0, Math.min(500, errorBody.length())));
}
```

Also add a null-guard on `getErrorStream()`, which returns `null` when the server returns no error body:

```java title="Null-guard"
java.io.InputStream errorStream = con.getErrorStream();
if (errorStream == null) {
    fail("Server returned HTTP " + responseCode + " with no error body — endpoint may not be reachable");
    return null;
}
```

!!! warning "Root cause: authentication filter intercepting the request"
    An HTML response on an endpoint that should return JSON usually means an authentication or security filter is intercepting the request before it reaches the servlet. For Etendo webhook endpoints (`/webhooks/*`), verify that:

    1. The `WebhookServiceHandler` servlet is registered in `web.xml` at `/webhooks/*`.
    2. Any authentication filter that wraps servlet requests has been fully migrated from `javax.servlet.*` to `jakarta.servlet.*`.
    3. The security bypass for the `/webhooks/*` URL pattern is still active after the migration (check `AD_MODEL_OBJECT_MAPPING` and any filter `url-pattern` excludes).

---

*More migration changes will be added to this guide as the migration progresses.*

---
This work is licensed under :material-creative-commons: :fontawesome-brands-creative-commons-by: :fontawesome-brands-creative-commons-sa: [ CC BY-SA 2.5 ES](https://creativecommons.org/licenses/by-sa/2.5/es/){target="_blank"} by [Futit Services S.L](https://etendo.software){target="_blank"}.
