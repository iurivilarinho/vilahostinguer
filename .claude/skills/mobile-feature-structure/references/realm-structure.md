# Feature Realm Structure

Each offline-first feature that persists domain data locally should own its own `realm/` folder.

---

## Folder layout

```text
feature/
  realm/
    adapters/
      mapFeatureApiToRealm.ts
      mapFeatureRealmToApi.ts
      mapFeatureRealmToForm.ts
    schemas/
      FeatureSchema.ts
    services/
      getFeatureById.ts
      upsertFeature.ts
```

---

## schemas/

Stores Realm schemas for the feature's specific entities.

```typescript
// task/realm/schemas/TaskSchema.ts
export class Task extends Realm.Object<Task> {
  id!: number;
  title!: string;
  completed!: boolean;

  static schema: Realm.ObjectSchema = {
    name: 'Task',
    primaryKey: 'id',
    properties: {
      id: 'int',
      title: 'string',
      completed: {type: 'bool', default: false},
    },
  };
}

export type UnmanagedTask = Realm.Unmanaged<Task>;
```

---

## services/

Stores Realm services related to this specific feature, such as reads and writes.

Examples:

- `getTaskById.ts`
- `listTasks.ts`
- `upsertTask.ts`
- `deleteTask.ts`

Keep each service focused. Prefer small files over a single giant Realm service.

---

## adapters/

Stores mappers and transformation helpers related to this feature's local models.

Adapters live directly inside `realm/adapters/` — **not** nested under `services/`.

Typical examples:

- Realm -> API
- API -> Realm
- Realm -> form
- form -> Realm

Examples:

- `mapTaskApiToRealm.ts`
- `mapTaskRealmToApi.ts`
- `mapTaskRealmToForm.ts`

Use adapters to avoid leaking Realm objects or remote DTOs directly into screens and components.

---

## Rules

- keep schemas, services, and adapters in separate folders
- adapters are a direct child of `realm/`, not nested under `services/`
- keep Realm code feature-local by default
- do not mix schema definitions with service logic
- do not place Realm adapters in `utils/` when they are feature-specific
- use adapters as the boundary between Realm models, API DTOs, and form models
