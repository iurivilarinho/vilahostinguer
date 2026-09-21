# Cookbook de debug — Web Push

Comandos prontos para diagnóstico, em ordem de uso.

## 1. Backend está vivo e VAPID carregou?

```bash
curl -s http://localhost:9091/push/public-key
```

**Esperado**: `{"publicKey":"BMsDSAvVUA…XPaY"}` (chave inteira, não-vazia, ~88 chars).

| Resposta | Diagnóstico |
|---|---|
| `{"publicKey":"B…"}` | OK — VAPID carregado, security liberada. |
| `{"publicKey":null}` ou `""` | `.env` não foi lido. Reinicia backend. Verifica `[push] PushService inicializado` no log de startup. |
| 401 `Invalid or expired token` | Falta whitelist em `SecurityFilter.shouldBypassAuthentication`. |
| 404 / connection refused | Backend não rodando ou em porta diferente. |

## 2. Quem está na porta 9091?

```bash
netstat -ano | grep ":9091"
```
```powershell
Get-Process -Id <PID>
```

`javaw.exe` = rodando via IDE. `java.exe` = via terminal/Maven.

## 3. POST de subscription manual

```bash
curl -s -X POST http://localhost:9091/push/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"endpoint":"https://fcm.googleapis.com/fcm/send/teste-fake","p256dh":"BO3Xq7kV0ZG9oKTeP2T7bC8NbXUQX1aQg5kJQjU0H6rZ8vYvL3dBRFL5OgWtT2gV4hN1pK6gJYqPzM5aXcYwJ0g","auth":"abcdef0123456789abcdef0123456789","topic":"customer","customerId":1}' \
  -w "\nHTTP %{http_code}\n"
```

204 = ok. 400 = valida `@NotBlank`. 401 = whitelist faltando.

## 4. DELETE manual

```bash
curl -s -X DELETE "http://localhost:9091/push/subscriptions?endpoint=https://fcm.googleapis.com/fcm/send/teste-fake" -w "%{http_code}\n"
```

## 5. SQL — listar subscriptions

```sql
SELECT id, topic, customer_id, created_at, substring(endpoint, 1, 60) AS endpoint_preview
FROM push_subscription
ORDER BY id DESC;
```

| Falha | Causa | Solução |
|---|---|---|
| Tabela não existe | Hibernate `ddl-auto=update` precisa ter rodado | Reinicia backend |
| 0 linhas após Ativar | POST `/push/subscriptions` falhou | F12 → Network do clique |
| `customer_id=NULL` em sub `customer` | Cliente não estava logado | Desativa, faz login, ativa |
| `customer_id` errado | Trocou de cliente sem desativar antes | Limpa: `DELETE FROM push_subscription WHERE customer_id=ANTIGO` |

## 6. Logs esperados no backend

### Startup
```
[push] PushService inicializado com VAPID. subject=mailto:..., publicKey=BMsDSAvVUA…
```
ou (se chaves não carregaram):
```
[push] VAPID keys ausentes — push notifications desativado.
```

### POST /push/subscriptions
```
[push] subscription salva id=N topic=customer customerId=1 endpointStart=https://fcm.go…
```

### Trigger de notificação
```
[push] notifyCustomer customerId=1 subscriptions=2 title=Seu pedido está pronto
[push] entregue id=5 status=201
[push] entregue id=6 status=410
[push] subscription expirada, removendo id=6
```

### Falhas
```
[push] notifyTopic ignorado (pushService=null) topic=tables ...   ← chaves faltando
[push] falha ao entregar id=N endpoint=https://...                ← stacktrace abaixo
```

## 7. Console (browser)

Após clicar Ativar:
```
[push] subscribed { endpoint: "https://fcm.…", p256dhLength: 87, authLength: 22, topic: "customer", customerId: 1 }
```

Em caso de erro:
```
[push] enable failed [DOMException: ...]
```

## 8. Smoke test isolado (sem backend)

DevTools → Application → Service Workers → campo "Push" → cola e clica botão Push:
```json
{"title":"Manual","body":"Sem backend","url":"/conta"}
```

| Resultado | Conclusão |
|---|---|
| Notificação aparece no SO | SW + permissão OK. Bug é no backend (não envia, ou envia errado). |
| Não aparece | Bug local: SW antigo cacheado, permissão denied, ou SO bloqueia. |

## 9. Reset total no browser (recomeço limpo)

1. F12 → Application → Service Workers → **Unregister**.
2. F12 → Application → Storage → **Clear site data**.
3. Cadeado na URL → Permissões → Notificações → **Restaurar padrão**.
4. Hard reload (Ctrl+Shift+R).
5. Tenta Ativar de novo.

Em aba anônima também funciona como reset, exceto que a permissão segue dependendo das configs do site.

## 10. Tabela de status do `pushService.send()`

| Status | Significado |
|---|---|
| 201 | Sucesso (FCM/Mozilla aceitou). |
| 400 | Payload inválido (verifica VAPID subject `mailto:` ou `https:`). |
| 401 | VAPID inválido — chave pública/privada não batem. Regenera. |
| 404, 410 | Subscription expirou. Backend remove sozinho. |
| 413 | Payload > 4KB. Reduz mensagem. |
| 429 | Rate limit do FCM. Backoff. |
