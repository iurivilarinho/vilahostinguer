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
| **Máquinas** | Máquinas Linux (Ubuntu, Debian, Alpine, Fedora, Rocky, Arch — só as que têm imagem para o processador) em contêineres Docker de sistema: versão, limite de CPU e memória, rede do dispositivo ou isolada com portas, pastas compartilhadas, usuário com sudo e SSH próprio. Ligar, desligar, reiniciar, terminal direto na máquina, saída, uso ao vivo. **Backup da máquina inteira** (`docker export` compactado, gravado neste PC), **restaurar** a partir dele, **reinstalar do zero** e **trocar a distribuição ou a versão** (com backup automático antes, se quiser). As pastas compartilhadas nunca são apagadas. |
| **Negócio** | Planos com preço por ciclo (mensal, trimestral, semestral, anual com desconto), clientes, contratos, faturas, configuração do painel do cliente e trilha de auditoria de tudo o que muda. |
| **Painel do cliente** | Site separado, no estilo do hPanel, em que o cliente se cadastra, escolhe um plano, paga por Pix e gerencia o próprio servidor: ligar/desligar, uso ao vivo, terminal no navegador, trocar sistema, backups, senha do SSH, faturas. Ver [Painel do cliente](#painel-do-cliente). |
| **Acesso remoto** | **Domínios com DDNS** (DuckDNS, Cloudflare com curinga, qualquer URL de atualização, ou manual só conferindo) atualizados quando o IP público muda. **Rotas**: sites por nome numa porta HTTP compartilhada (pelo cabeçalho Host), sites HTTPS repassados pelo nome da conexão (SNI, o certificado fica na máquina) e portas TCP (SSH, bancos, jogos) — o PC recebe e repassa ao dispositivo ou máquina, porque aparelhos no cabo USB não são alcançáveis de fora. Abre as portas no roteador por UPnP (opcional), detecta CGNAT e mostra o tráfego de cada rota. |
| **Atividades** | Toda operação (instalar, remover, serviço, atualizar, backup, restaurar, formatar) com a saída completa, ao vivo enquanto roda, e cancelamento. |

## Arquitetura

```
Bancada.exe (Tauri, janela nativa + bandeja + início automático)
 └─ bancada-backend.exe (jpackage: JRE + jar) — Spring Boot em 127.0.0.1:8747
     ├─ serve o React (build do Vite dentro do jar)
     ├─ REST em /api/**, SSE em /api/events, WebSocket em /ws/terminal
     ├─ SQLite em ~/.bancada/bancada.db
     ├─ SSH (JSch) para os dispositivos
     ├─ painel do cliente em 127.0.0.1:8748 (/api/portal, /ws/portal, portal.html)
     └─ gateway de acesso remoto: portas abertas em 0.0.0.0 só para as rotas ativas
        e para o painel do cliente (HTTP e, com certificado, HTTPS)
```

- A casca gera um **token de sessão** a cada abertura e o passa ao backend por variável de
  ambiente; a API só responde a quem apresenta o token. O backend escuta só em `127.0.0.1`.
- Fechar a janela só a esconde na bandeja: a detecção continua, e aparelho conectado com a janela
  escondida vira notificação do Windows.
- A chave do servidor SSH de cada dispositivo é fixada no cadastro: se ela mudar, a conexão é
  recusada em vez de confiar em silêncio.
- Comandos que exigem root usam `sudo -n` quando a credencial não é root.
- A API continua só em `127.0.0.1`; o que fica exposto na rede são apenas as portas das rotas
  ativas, e cada conexão vai só para o destino daquela rota. Tokens de DNS são cifrados como as
  credenciais.

### Pastas

```
backend/                      Spring Boot (Java 17)
  src/main/java/com/bancada/  models, repository, request, records, response, filter,
                              specification, service, controller, enums, config, exception
  src/main/java/com/bancada/gateway/  repasse de conexões (HTTP por Host, HTTPS por SNI, TCP)
  src/main/resources/scripts/ scripts POSIX executados nos aparelhos (facts, metrics, partitions,
                              files, docker-setup)
src/                          React 19 + Vite + Tailwind 4
  app/                        providers, rotas, layout, eventos em tempo real
  components/                 componentes reutilizáveis (cada um com .stories.tsx)
  features/                   devices, credentials, terminal, apps, files, backups, storage,
                              machines, remote-access, operations, settings, dashboard,
                              public/design-system, business (planos, clientes, vendas,
                              auditoria), portal (as telas do painel do cliente)
  app/portal/                 casca, rotas e layout do painel do cliente (entrada portal.html)
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
`BANCADA_DB`, `BANCADA_TOKEN`, `BANCADA_PORTAL_PORT` (8748). No `npm run dev`, o painel do cliente
fica em `http://localhost:5173/portal.html`.

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

### Docker em kernel antigo

Instalar o Docker pelo painel já prepara aparelhos com kernel 3.x (o J4+ roda 3.18). O kernel
precisa ter namespaces, cgroups (memory, devices, cpuset), veth, bridge e NAT — no J4+ isso exigiu
recompilar com essas opções. Além disso, o `docker-setup.sh`:

- monta os cgroups v1 e passa o OpenRC para `rc_cgroup_mode="legacy"` (sem cgroup2 o modo padrão
  não monta nada e o dockerd recusa: *Devices cgroup isn't mounted*);
- troca para o `iptables-legacy` quando o backend nftables não funciona (*Could not fetch rule set
  generation id*);
- instala o `runc` 1.3 oficial (estático, com sha256 conferido) quando o da distribuição usa
  `libpathrs`, que aborta em kernel anterior ao 5.2 (*at least one candidate /proc/thread-self
  path should work*);
- usa o driver `vfs` (overlay2 pede kernel 4.0+) e guarda imagens em `/srv/docker` quando a raiz é
  pequena; leva o proxy HTTP do aparelho para o dockerd.

Sem cota de CPU (CFS) no kernel, o limite de CPU da máquina vira peso proporcional (`--cpu-shares`).

Outros detalhes que só aparecem nesse kernel:

- **Rede isolada (bridge)**: funciona, mas o proxy HTTP do aparelho escuta em `127.0.0.1`. O
  script instala `/usr/local/sbin/bancada-bridge-proxy` (reaplicado no boot por `/etc/local.d`),
  que liga `route_localnet` e redireciona `gateway-do-docker0:8899` para o proxy; as máquinas
  isoladas saem para a internet por ali.
- **umask 000 no `docker exec`**: tudo o que o painel roda dentro da máquina passa por
  `umask 022`, e `/run/sshd` é corrigido, senão o sshd do Debian/Ubuntu recusa subir por
  diretório gravável por todos.
- **Rede do dispositivo**: a máquina recebe `--add-host nome:127.0.1.1` para o `sudo` resolver o
  próprio nome.

### Acesso de fora

1. Cadastre um domínio (o DuckDNS é grátis) e ligue o DDNS.
2. Crie rotas: `blog.casa.duckdns.org` → máquina, porta 80; porta 2201 do PC → SSH da máquina.
3. No roteador, redirecione as portas que aparecem na visão geral para o IP deste PC (ou ligue o
   UPnP), e permita o Bancada no Firewall do Windows.
4. Com CGNAT (a visão geral avisa), nenhum redirecionamento funciona: é preciso IP público da
   operadora ou um túnel.

## Painel do cliente

O Bancada também vende os servidores. O cliente acessa um site próprio (não o painel de admin), cria
a conta, contrata um plano e gerencia a máquina sozinho — como no hPanel da Hostinger.

**Como fica publicado.** O backend abre um segundo conector em `127.0.0.1:8748` que só responde
`/api/portal/**`, `/ws/portal/**`, `portal.html`, os assets e o desafio do ACME; a API do admin
nunca passa por essa porta. O gateway expõe esse conector na porta HTTP das rotas e, depois que o
certificado existe, na porta HTTPS, terminando o TLS no próprio PC. Em **Negócio → Painel do
cliente** ficam o domínio do painel, o e-mail do Let's Encrypt (o certificado sai pelo desafio
HTTP-01 e é renovado sozinho 30 dias antes de vencer), o nome da empresa, a faixa de portas dos
clientes, o domínio dos sites e as regras de cobrança.

**Contratar.** O cliente escolhe plano, ciclo, sistema e nome do servidor e define a senha do root.
Nasce um contrato *aguardando pagamento* com a primeira fatura. A fatura é paga por:

- **Pix do Mercado Pago**, com o access token configurado: o painel mostra QR Code e copia-e-cola,
  e o pagamento é conferido a cada minuto (ou no botão "Já paguei");
- **manual**: sem token, o cliente vê as instruções e o administrador confirma em **Negócio →
  Faturas**.

Paga a fatura, o servidor é criado sozinho no dispositivo do plano: máquina em rede isolada, com
três portas da faixa (SSH, 80 e 443) e as rotas correspondentes — com domínio de sites configurado,
o site fica em `nome.dominio`.

**Ciclo de cobrança** (a cada 10 minutos): fatura de renovação alguns dias antes do vencimento;
atraso além do limite suspende (a máquina é desligada e as rotas saem do ar); atraso maior cancela
e apaga o servidor; pedidos não pagos em 3 dias são cancelados. Pagar uma fatura atrasada reativa.
O cliente pode cancelar no fim do período ou desistir do cancelamento.

**No painel o cliente**: vê o resumo, as faturas e os servidores; liga, desliga e reinicia; vê o
uso ao vivo; abre o terminal no navegador; reinstala ou troca o sistema; faz, baixa, restaura e
descarta backups (limite por plano); troca a senha do SSH; edita os dados e a senha da conta.

**Segurança.**

- Senhas com BCrypt; sessão em JWT num cookie `httpOnly` `SameSite=Lax` (`Secure` quando veio por
  HTTPS), invalidada ao trocar a senha ou ao bloquear o cliente.
- 5 tentativas erradas por e-mail ou 20 por IP bloqueiam o login por 15 minutos.
- Cada recurso é conferido contra o dono; o de outro cliente responde como inexistente (404).
- O WebSocket do terminal só aceita a mesma origem.
- Erros internos voltam como mensagem genérica.
- Toda mudança de cliente, plano, contrato, fatura e ação no servidor fica na **auditoria**, com o
  antes e o depois.

**Limites que valem conhecer.**

- Não há envio de e-mail: a recuperação de senha é pelo suporte (o admin redefine em
  **Clientes**).
- As máquinas são contêineres num kernel compartilhado (3.18 no J4+, sem as proteções dos kernels
  novos). Serve para clientes conhecidos, não para estranhos com root que podem tentar escapar do
  contêiner.
- Para o painel e os servidores serem vistos de fora valem as mesmas condições de
  [Acesso de fora](#acesso-de-fora): portas redirecionadas e sem CGNAT.

## Decisões em relação às skills do projeto

- **Status enums** ficam um por arquivo em `enums/` (o projeto não tem `Types.java`).
- **`Specification.where(null)`** no lugar de `Specification.unrestricted()`: o método não existe no
  Spring Data JPA 3.3 usado aqui; o efeito é o mesmo.
- **Texto longo** (saída de operações) usa `columnDefinition = "text"` em vez de `@Lob`, que o driver
  SQLite não lê como CLOB.
- **Arquivos** por comandos de shell em vez de SFTP, porque dropbear costuma vir sem `sftp-server`.
- **Enums no SQLite**: o Hibernate cria um `CHECK (coluna in (...))` para cada enum e o
  `ddl-auto: update` nunca o atualiza, então um valor novo seria recusado em bancos antigos. O
  `EnumCheckCleaner` reconstrói as tabelas sem esses `CHECK` antes do Hibernate subir.
- **Gateway** fica em `gateway/` (infraestrutura de sockets), fora de `service/`, que só tem
  classes `@Service`.
- **Painel do cliente no mesmo projeto**: é uma segunda página do Vite (`portal.html`, entrada
  `src/portal-main.tsx`) com casca e rotas próprias em `app/portal/` e telas em
  `features/portal/`. Os componentes, o design system e o cliente HTTP são os mesmos; o bundle do
  admin não entra no do cliente.
- **Duas autenticações**: o admin continua com o token de sessão da casca (`AppTokenFilter`); o
  Spring Security só cuida de `/api/portal/**` e `/ws/portal/**`, sem sessão, com o filtro de JWT.
- **Auditoria** segue a skill de audit log: registro só de inclusão, com fotografias em records
  (`*Snapshot`) e `createdBy`/`updatedBy` nas entidades do negócio.
- **`transaction_mode=IMMEDIATE`** no SQLite: com o padrão (DEFERRED), uma transação que lia e
  depois escrevia falhava com `SQLITE_BUSY_SNAPSHOT` quando outra conexão escrevia no meio, o que
  derrubava operações em segundo plano.
- **Eventos depois do commit** (`@TransactionalEventListener`) não gravam nada no próprio
  listener — o que ele escreve se perde; o trabalho vai para o executor de operações.
