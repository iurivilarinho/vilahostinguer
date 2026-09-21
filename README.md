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
| **Máquinas** | Máquinas Linux (Ubuntu, Debian, Alpine, Fedora, Rocky, Arch — só as que têm imagem para o processador) em contêineres Docker de sistema: versão, limite de CPU e memória, rede do dispositivo ou isolada com portas, pastas compartilhadas, usuário com sudo e SSH próprio. Ligar, desligar, reiniciar, terminal direto na máquina, saída, uso ao vivo. |
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
     └─ gateway de acesso remoto: portas abertas em 0.0.0.0 só para as rotas ativas
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
                              public/design-system
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

### Acesso de fora

1. Cadastre um domínio (o DuckDNS é grátis) e ligue o DDNS.
2. Crie rotas: `blog.casa.duckdns.org` → máquina, porta 80; porta 2201 do PC → SSH da máquina.
3. No roteador, redirecione as portas que aparecem na visão geral para o IP deste PC (ou ligue o
   UPnP), e permita o Bancada no Firewall do Windows.
4. Com CGNAT (a visão geral avisa), nenhum redirecionamento funciona: é preciso IP público da
   operadora ou um túnel.

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
