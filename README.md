# Bancada

Painel de desktop para servidores caseiros — celulares com postmarketOS, placas (Raspberry Pi,
Orange Pi), mini PCs — no estilo dos painéis de hospedagem. Conectou o aparelho no cabo USB, ele
aparece sozinho: o painel lê as informações do sistema, guarda tudo e dá terminal, aplicativos,
arquivos, backups e armazenamento sem abrir outro programa.

## O que faz

| Área | O que tem |
|---|---|
| **Detecção automática** | A cada poucos segundos olha os adaptadores de rede USB (link-local `169.254.x.x`, NCM/RNDIS), a tabela ARP e endereços extras; testa a porta 22 e identifica o aparelho pela **chave do servidor SSH**, não pelo IP. Aparelho novo vira cadastro e notificação; se houver credencial padrão, é configurado sozinho. |
| **Dispositivos** | Grade com uso de CPU, memória e disco ao vivo; página de cada um com visão geral (recursos em tempo real, gráfico, bateria, temperatura, informações do sistema), terminal, aplicativos, arquivos, backups, armazenamento, atividades e configurações. Cadastro manual pelo endereço para máquinas na rede. Arquivar em vez de apagar. |
| **Credenciais** | Cofre de usuário/senha ou chave privada (com senha opcional). O segredo é cifrado com o **DPAPI do Windows** antes de ir para o banco; revelar é uma ação explícita. Uma credencial pode ser a padrão para aparelhos novos. |
| **Terminal** | Shell de verdade (PTY via SSH) no xterm.js, com redimensionamento, cores e UTF-8. Várias sessões em abas. |
| **Aplicativos** | Catálogo com Nginx, Caddy, Node.js, Java 17/21, Python, PostgreSQL, MariaDB, Redis, Docker, Git, Certbot e ferramentas básicas. Instala pelo gerenciador do próprio sistema (apk, apt, dnf, pacman), ativa e inicia o serviço (OpenRC ou systemd); iniciar/parar/reiniciar/ativar no boot; atualizar o sistema. |
| **Arquivos** | Navegar, enviar, baixar, criar pasta e apagar (pastas só vazias). Funciona com dropbear sem sftp-server. |
| **Backups** | `tar.gz` das pastas escolhidas, gerado no aparelho e gravado direto neste computador (nada fica no aparelho), com SHA-256. Baixar, restaurar com um clique, descartar (o registro fica). |
| **Armazenamento** | Discos e partições com uso. Formatação (ext4/FAT32) só de partições de dados; as de sistema do aparelho (boot, modem, efs, persist…) e as montadas ficam protegidas, e a confirmação exige digitar o nome. Em kernel antigo (3.x), o ext4 sai sem `metadata_csum_seed`/`orphan_file` para ele conseguir montar. |
| **Atividades** | Toda operação (instalar, remover, serviço, atualizar, backup, restaurar, formatar) com a saída completa, ao vivo enquanto roda, e cancelamento. |

## Arquitetura

```
Bancada.exe (Tauri, janela nativa + bandeja + início automático)
 └─ bancada-backend.exe (jpackage: JRE + jar) — Spring Boot em 127.0.0.1:8747
     ├─ serve o React (build do Vite dentro do jar)
     ├─ REST em /api/**, SSE em /api/events, WebSocket em /ws/terminal
     ├─ SQLite em ~/.bancada/bancada.db
     └─ SSH (JSch) para os dispositivos
```

- A casca gera um **token de sessão** a cada abertura e o passa ao backend por variável de
  ambiente; a API só responde a quem apresenta o token. O backend escuta só em `127.0.0.1`.
- Fechar a janela só a esconde na bandeja: a detecção continua, e aparelho conectado com a janela
  escondida vira notificação do Windows.
- A chave do servidor SSH de cada dispositivo é fixada no cadastro: se ela mudar, a conexão é
  recusada em vez de confiar em silêncio.
- Comandos que exigem root usam `sudo -n` quando a credencial não é root.

### Pastas

```
backend/                      Spring Boot (Java 17)
  src/main/java/com/bancada/  models, repository, request, records, response, filter,
                              specification, service, controller, enums, config, exception
  src/main/resources/scripts/ scripts POSIX executados nos aparelhos (facts, metrics, partitions, files)
src/                          React 19 + Vite + Tailwind 4
  app/                        providers, rotas, layout, eventos em tempo real
  components/                 componentes reutilizáveis (cada um com .stories.tsx)
  features/                   devices, credentials, terminal, apps, files, backups, storage,
                              operations, settings, dashboard, public/design-system
  lib/                        cliente da API, tipos de paginação, formatação
src-tauri/                    casca nativa (Rust)
```

## Desenvolvimento

Pré-requisitos: JDK 17, Maven, Node 20+, Rust (para a casca).

```powershell
# backend em 8747 (sem token: a API fica aberta só para desenvolvimento)
cd backend; mvn spring-boot:run

# frontend em 5173, com proxy de /api e /ws para 8747
npm install
npm run dev

# casca nativa apontando para o backend já rodando
npm run tauri dev
```

- Swagger: `http://127.0.0.1:8747/swagger`
- Storybook: `npm run storybook`
- Testes: `npm test` (frontend) e `mvn test` (backend)

Variáveis de ambiente do backend: `BANCADA_PORT` (8747), `BANCADA_DATA` (`~/.bancada`),
`BANCADA_DB`, `BANCADA_TOKEN`.

## Empacotar

```powershell
powershell -ExecutionPolicy Bypass -File package.ps1
# -> src-tauri\target\release\bundle\nsis\Bancada_0.1.0_x64-setup.exe
```

Use `MAVEN_CMD` para apontar um Maven que não esteja no PATH.

## Preparando um aparelho

O painel espera SSH ativo e rede pelo cabo. Nos celulares com postmarketOS configurados como no
projeto [postmarketOS no Galaxy J4+](https://github.com/iurivilarinho/postmarketOS), o aparelho fica
em `169.254.1.1` pelo cabo USB (NCM) e é detectado sozinho.

## Decisões em relação às skills do projeto

- **Status enums** ficam um por arquivo em `enums/` (o projeto não tem `Types.java`).
- **`Specification.where(null)`** no lugar de `Specification.unrestricted()`: o método não existe no
  Spring Data JPA 3.3 usado aqui; o efeito é o mesmo.
- **Texto longo** (saída de operações) usa `columnDefinition = "text"` em vez de `@Lob`, que o driver
  SQLite não lê como CLOB.
- **Arquivos** por comandos de shell em vez de SFTP, porque dropbear costuma vir sem `sftp-server`.
