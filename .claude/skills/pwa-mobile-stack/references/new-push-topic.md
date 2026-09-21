# Adicionar um novo topic de push

Topics atuais: `customer`, `kitchen`, `tables`. Cada um tem trigger no backend e botão `<PushNotificationsToggle>` em uma página específica do front.

Para adicionar um topic novo (ex.: `manager` para alertas gerenciais), siga **exatamente** os passos abaixo na ordem.

## 1. Backend — adicionar a constante

`backend-comandas/src/main/java/com/br/food/models/PushSubscription.java`:

```java
public static final String TOPIC_MANAGER = "manager";
```

(O índice em `topic` já existe, não precisa migrar nada.)

## 2. Backend — adicionar o trigger

Encontre o método de service que dispara o evento de negócio. Injete `PushNotificationService` no construtor (já está em `OrderService` e `KitchenService` como exemplos):

```java
private final PushNotificationService pushNotificationService;

public XxxService(..., PushNotificationService pushNotificationService) {
  this.pushNotificationService = pushNotificationService;
}

@Transactional
public Xxx alguma_acao(...) {
  // ... lógica ...

  pushNotificationService.notifyTopic(
      PushSubscription.TOPIC_MANAGER,
      "Título da notificação",
      "Corpo descritivo, ex.: " + algumaMetrica + " ultrapassou limite.",
      "/admin/dashboard");  // URL para onde abre ao clicar
  return saved;
}
```

Para notificação por cliente (usa `customer_id` em vez de topic), use `notifyCustomer(customerId, title, body, url)`.

## 3. Frontend — adicionar ao tipo

`frontend-comandas/src/api/services/push.ts`:

```ts
export type PushTopic = "kitchen" | "customer" | "tables" | "manager";
```

## 4. Frontend — montar o toggle na página

```tsx
import { PushNotificationsToggle } from "@/components/pwa/PushNotificationsToggle";

// dentro da render da página:
<PushNotificationsToggle
  topic="manager"
  label="Notificar alertas gerenciais"
  description="Receba avisos de KPIs fora do esperado."
/>
```

Para `topic="customer"`, sempre passar `customerId={customerId}` extraído de `getStoredCustomerId()`. Sem o `customerId`, `notifyCustomer(null)` ignora.

## 5. Verificar

1. Reiniciar o backend (Spring DevTools recompila quando `target/classes` muda; se rodar pela IDE, force).
2. Reiniciar o frontend (`npm run dev`).
3. Logar no role correto e clicar Ativar no novo banner.
4. Banco:
   ```sql
   SELECT id, topic, customer_id FROM push_subscription WHERE topic = 'manager';
   ```
   Deve aparecer 1 linha após o clique.
5. Disparar a ação que chama `notifyTopic`.
6. Backend log esperado:
   ```
   [push] notifyTopic topic=manager subscriptions=1 title=...
   [push] entregue id=N status=201
   ```
7. Notificação cai no SO da máquina inscrita.

## Pegadinhas comuns

- **Topic em branco/typo**: cria `topic='Manager'` (case sensitive) e `notifyTopic("manager", ...)` retorna 0 subscriptions. Use sempre `PushSubscription.TOPIC_*`.
- **Trigger dentro de `@Transactional`**: ok, o `notifyTopic` faz POST HTTP para FCM/Mozilla — não roda no commit do banco. Se a transação reverter, o push ainda foi enviado. Para garantir que só envia após commit, use `TransactionSynchronizationManager.registerSynchronization` com afterCommit. Para casos comuns isso é over-engineering; aceitar.
- **Spam de push**: se o trigger é chamado em loop (ex.: dentro de `for`), você dispara N notificações. Sempre verificar se a chamada está fora do loop ou debounced.
