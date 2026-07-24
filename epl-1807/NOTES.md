# EPL-1807 — Design Notes

## Target_ID_Resolver_SQL: how it refers to the firing record

`AD_COLUMN_COMP_DEPENDENCY.TARGET_ID_RESOLVER_SQL` (VARCHAR 4000, required) is embedded
**verbatim** inside the generated PL/pgSQL trigger function, within a
`FOR v_target_id IN ( {TARGET_ID_RESOLVER_SQL} ) LOOP`. Because the SQL is inlined into the
trigger function body, it executes in a scope where the standard PostgreSQL trigger
pseudo-records are available:

- `NEW` — the row after INSERT / UPDATE (NULL on DELETE)
- `OLD` — the row before UPDATE / DELETE (NULL on INSERT)
- `TG_OP`, `TG_TABLE_NAME` — operation and table context

That is how the resolver "refers to the record that caused the trigger to fire": it reads
`NEW`/`OLD` field values directly. The generator never needs to understand the source schema —
all table-specific knowledge lives in the per-dependency resolver SQL. `NEW`/`OLD` are the
contract between the trigger runtime and that SQL.

The `FOR … LOOP` iterates over **0, 1, or N** rows the resolver returns, writing one
`AD_STOREDCOLUMN_DIRTY` record per returned target id (with `ON CONFLICT DO NOTHING`). So the
resolver can resolve a single changed row to none, one, or many target records.

## Pattern 1 — single target (immutable mapping)

When the changed row maps to exactly one target and the mapping column does not change on update:

```sql
SELECT COALESCE(NEW.c_order_id, OLD.c_order_id)
```

`COALESCE` picks `NEW` on INSERT/UPDATE and falls back to `OLD` on DELETE.

## Pattern 2 — both NEW and OLD on update (reparenting)

When the resolver walks an FK that **can be reassigned** on update (a line moved to another
order, a payment moved to another invoice, etc.), a single update is genuinely a **two-target**
event: the old parent's aggregate is now stale (a child left) and the new parent's aggregate is
now stale (a child arrived). Both must be recomputed. `COALESCE` would pick only one and silently
leave the other stale.

Return both, and let `UNION` collapse them when equal:

```sql
SELECT NEW.c_order_id WHERE NEW.c_order_id IS NOT NULL
UNION
SELECT OLD.c_order_id WHERE OLD.c_order_id IS NOT NULL
```

Behavior across operations:

| Operation             | NEW.c_order_id | OLD.c_order_id | Rows returned                     |
|-----------------------|----------------|----------------|-----------------------------------|
| INSERT                | value          | NULL           | just NEW                          |
| DELETE                | NULL           | value          | just OLD                          |
| UPDATE, unchanged     | X              | X              | one row (UNION dedups)            |
| UPDATE, **reparented**| B              | A              | two rows → both A and B dirty     |

Two things do the work:

1. **`UNION` (not `UNION ALL`)** — eliminates the duplicate when `NEW = OLD`, so an ordinary
   update that doesn't move the row enqueues a single dirty record and keeps the queue clean at
   the source. (`AD_STOREDCOLDIRTY_DEDUP` would absorb a duplicate anyway, but `UNION` avoids
   generating it in the first place.)
2. **The `WHERE … IS NOT NULL` guards** — these make the *same* resolver safe for INSERT and
   DELETE. On INSERT `OLD` is a NULL record so the second branch yields nothing; on DELETE `NEW`
   is NULL so the first branch yields nothing. Without the guards a NULL `v_target_id` would be
   enqueued.

## Choosing between the patterns (convention)

This is a correctness decision the dependency author can get wrong, so it should be documented in
the field help / design doc rather than left implicit:

- Resolver maps to the target's PK via a column that is **immutable** → `COALESCE(NEW.x, OLD.x)`.
- Resolver walks an FK that **can change on update** → `UNION` form, or every reparenting update
  corrupts one aggregate.

## No code change needed

The trigger function body already supports all of this:
`FOR v_target_id IN ( {TARGET_ID_RESOLVER_SQL} ) LOOP` handles 0/1/2/N rows. The pattern choice
is purely a matter of what SQL is stored in `TARGET_ID_RESOLVER_SQL` — no change to
`GenerateStoredComputedTriggers.java`. Add both patterns as canonical examples in the field's
help text and the EPL-1807 design doc.
