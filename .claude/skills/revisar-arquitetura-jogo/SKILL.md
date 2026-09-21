---
name: revisar-arquitetura-jogo
description: >
  Revisa a arquitetura e o código de um jogo/app 3D interativo (especialmente
  Three.js/WebGL/Tauri/JS) contra padrões de mercado: design patterns de jogos
  (Game Programming Patterns / Nystrom), boas práticas de Three.js (draw calls,
  dispose/memória, instancing, asset pipeline), separação de responsabilidades,
  composição vs herança, ECS, game loop, gerência de estado e qualidade de código.
  Use quando o usuário pedir para REVISAR/AUDITAR a arquitetura, o código, a
  performance ou a "saúde" de um jogo ou app 3D; refatorar; ou avaliar contra
  boas práticas. Gera um relatório com achados priorizados (Crítico/Alto/Médio/Baixo),
  arquivo:linha, o padrão violado e a correção recomendada.
---

# Revisão de arquitetura/código de jogo (3D / Three.js)

Objetivo: revisar um jogo ou app 3D contra **padrões de mercado**, achar problemas
de arquitetura/performance/qualidade, e entregar um **relatório acionável**
(achado → impacto → padrão de referência → correção). Não é só "achar bug" — é
avaliar a ESTRUTURA contra o que a indústria considera boa prática.

## Como conduzir a revisão (método)

1. **Mapear** a estrutura: pontos de entrada, módulos, fluxo de dados, o game loop,
   onde mora o estado, como assets são carregados/descartados. (`Glob`/`Grep`/`Read`;
   para varredura ampla, despache um agente `Explore`.)
2. **Medir** tamanho/acoplamento: arquivos gigantes (>500 linhas = sinal de "god module"),
   funções longas (>60 linhas), nº de responsabilidades por módulo, imports cruzados.
3. **Auditar** por eixo (seções abaixo): Arquitetura, Design Patterns, Three.js/Perf,
   Estado/Loop, Qualidade. Para cada, citar `arquivo:linha`.
4. **Priorizar** cada achado: **Crítico** (vaza memória / quebra / trava FPS),
   **Alto** (acoplamento que impede evoluir), **Médio** (duplicação/legibilidade),
   **Baixo** (estilo).
5. **Recomendar**: o padrão/refactor concreto, com exemplo curto. Não reescrever tudo —
   apontar o caminho e os 3–5 movimentos de maior alavancagem.
6. Provar com evidência (renderer.info, perfil, contagem de linhas) quando possível;
   não afirmar "vaza memória" sem mostrar o recurso não-descartado.

## Eixo 1 — Arquitetura & separação de responsabilidades

- **Single Responsibility / módulos coesos.** Cada arquivo/classe = uma razão pra mudar.
  Arquivo de 1000+ linhas que faz input + animação + física + render + UI é um
  **god module** → fatiar por domínio. (Ex. clássico: um `character.js` que vira
  monolito — separar input, locomoção, retarget de animação, câmera.)
- **Composição > herança.** Hierarquias profundas de subclasses (`Player extends
  Character extends Entity ...`) endurecem. Prefira **Component** (compor comportamento
  de peças reusáveis) — base do **ECS** (entidade = id; componentes = dados; sistemas =
  lógica que varre entidades com certos componentes). Para jogos com muitas entidades,
  ECS é cache-friendly e flexível; para UI/menus, OOP simples basta (modelo híbrido é
  o padrão de mercado).
- **Camadas / fronteiras de módulo.** Render NÃO deve conhecer regras de jogo; lógica
  de jogo NÃO deve chamar `THREE.*` direto espalhado. Isole o motor (Three.js) atrás de
  uma camada fina — facilita testar, trocar (ex.: WebGPU) e raciocinar.
- **Data-driven.** Cenas/entidades/skins definidas em **dados** (JSON) e não hard-coded
  (ver **Type Object**). Bom sinal: adicionar conteúdo = editar dado, não código.
- **Sem números mágicos** espalhados; constantes/config nomeadas e centralizadas.
- **Acoplamento via eventos** onde fizer sentido (ver Observer/Event Queue) em vez de
  módulos chamando uns aos outros diretamente.

## Eixo 2 — Design Patterns de jogos (Game Programming Patterns, Nystrom)

Use como vocabulário pra nomear problemas e soluções. Por categoria:

**Sequenciamento**
- **Game Loop** — loop que separa processar input → atualizar estado → renderizar.
  Revisar: o loop usa **delta-time / timestep fixo** (física estável independente do FPS)
  ou assume frames constantes? Tudo amarrado no `requestAnimationFrame` sem `dt`?
- **Update Method** — cada entidade se atualiza por frame via `update(dt)` uniforme,
  em vez de lógica espalhada/condicional gigante.
- **Double Buffer** — render/estado em buffers separados (o WebGL já faz; relevante p/
  estado de jogo que muda durante o update).

**Comportamento**
- **State / State Machine** — comportamento por estado (idle/andando/pulando/voando).
  Cheirou `if/else`/flags booleanas combinatórias pra modo do personagem? → máquina de
  estados explícita.
- **Type Object / Subclass Sandbox** — tipos/skins definidos por dado em runtime; base
  segura pra variações sem duplicar código.

**Desacoplamento**
- **Component** — comportamento composto de peças (a espinha do ECS).
- **Observer / Event Queue** — emissor não conhece receptor; eventos desacoplam no tempo.
  Bom pra input, UI, conquistas, som. Revisar: callbacks/ponteiros diretos entre
  subsistemas que deveriam ser eventos.
- **Service Locator / Singleton** — acesso a serviços (renderer, áudio, assets) sem
  acoplar; CUIDADO: singleton vira estado global escondido — usar com parcimônia.

**Design patterns clássicos relevantes**
- **Command** — input como objetos (rebind de teclas, replay, undo); evita ler teclado
  espalhado pela lógica.
- **Flyweight** — compartilhar dados comuns entre muitos objetos (ex.: clips de animação,
  materiais, geometrias) em vez de duplicar por instância.
- **Prototype** — instanciar clonando um modelo pronto.

**Otimização**
- **Object Pool** — reusar objetos (projéteis, partículas, inimigos) em vez de
  alocar/destruir no loop (evita GC stutter). Em JS/Three.js: reusar `Vector3`/`Matrix4`
  temporários, poolar meshes recicláveis.
- **Dirty Flag** — só recalcular o que mudou (transform world, bounding box) — Three.js
  usa `matrixAutoUpdate`/`matrixWorldNeedsUpdate`; revisar updates redundantes por frame.
- **Spatial Partition** — grid/octree/BVH pra consultas de proximidade/colisão em vez de
  O(n²) varrendo tudo.
- **Data Locality** — layout de dados contíguo (arrays tipados) pra cache; o "porquê" do ECS.

## Eixo 3 — Three.js / WebGL (performance & memória)

**Draw calls** (meta: <100/frame, ideal <50)
- Muitos objetos repetidos (árvores, props, partículas) → **InstancedMesh**; mesma textura
  e geometrias diferentes → **BatchedMesh**. Instancing corta draw calls 90%+.
- Fundir geometrias estáticas (merge) que nunca se movem.
- Menos materiais distintos = menos troca de estado.

**Memória / dispose (vazamento é o bug nº1)**
- Ao remover do scene: `geometry.dispose()`, `material.dispose()`, `texture.dispose()`
  (e p/ textura de glTF ImageBitmap: `texture.source.data.close?.()`).
- Monitorar `renderer.info.memory` — se `geometries`/`textures` só crescem, há vazamento.
  Trocar de personagem/cena repetidamente e ver se o número estabiliza.
- Listeners (`addEventListener`, resize), `requestAnimationFrame`, e timers precisam ser
  removidos ao destruir a cena.

**Assets / pipeline**
- glTF/GLB comprimido: **Draco** (geometria) + **KTX2/Basis** (textura). Modelos crus
  pesados travam o load.
- Reusar materiais/geometrias/clips entre instâncias (Flyweight); não recarregar o mesmo
  GLB N vezes.
- Texturas em potência de 2 quando possível; mipmaps; `colorSpace` correto.

**Render loop**
- Não criar objetos por frame (Vector3/Material/array) → pool/reuse (evita GC).
- `renderer.setPixelRatio(Math.min(devicePixelRatio, 2))` — não renderizar 3x em telas
  retina sem necessidade.
- Pausar o loop quando a aba/janela não está visível.
- Sombras e pós-processamento são caros — orçar.

**Futuro:** WebGPU já é viável em todos os browsers grandes (2025) — 2–10x em cenas com
muitos draw calls/compute. Vale avaliar a fronteira do motor pensando nisso.

## Eixo 4 — Estado, loop e tempo

- **Fonte única de verdade** do estado do jogo (não estado duplicado/dessincronizado
  entre módulos e o scene graph).
- **Delta-time** em tudo que é movimento/animação; idealmente **timestep fixo** pra
  simulação + interpolação no render.
- **Carregamento assíncrono** sem travar o loop; estados de loading explícitos.
- Persistência (save/scene.json) versionada e validada ao carregar.

## Eixo 5 — Qualidade de código

- Funções pequenas e nomeadas; sem mega-funções com muitos `if`.
- Sem duplicação (DRY) — extrair helpers (ex.: lógica repetida de bbox/normalização).
- Erros tratados (load falho de asset, GLB inválido) — não engolir silenciosamente.
- Nomes consistentes; comentários explicam o PORQUÊ, não o óbvio.
- Testabilidade: lógica de jogo separável do render dá pra testar sem GPU.
- Sem segredos/credenciais commitados.

## Formato do relatório (saída)

```
# Revisão de arquitetura — <app>
## Resumo (3-5 linhas: saúde geral + 3 maiores alavancas)
## Achados
[CRÍTICO] <título> — arquivo:linha
  Problema: ...   Padrão: <ex. Object Pool / dispose>   Correção: ...
[ALTO] ...
[MÉDIO] ...
[BAIXO] ...
## Refactors de maior alavancagem (top 3-5, ordenados por custo/benefício)
## O que já está BOM (reforçar o que não regredir)
```

## Checklist rápido pra apps Three.js (cole e marque)
- [ ] Arquivos > 500 linhas? quais responsabilidades misturam?
- [ ] Game loop usa delta-time? timestep fixo p/ simulação?
- [ ] Estado do personagem = máquina de estados ou flags soltas?
- [ ] Composição/ECS vs herança profunda?
- [ ] Render isolado da lógica de jogo? `THREE.*` espalhado?
- [ ] `dispose()` de geometry/material/texture ao remover? listeners/RAF limpos?
- [ ] `renderer.info` estável ao trocar cena/skin (sem vazar)?
- [ ] Draw calls < 100? instancing pra repetidos?
- [ ] Assets comprimidos (Draco/KTX2)? materiais/clips reusados (Flyweight)?
- [ ] Objetos alocados por frame no loop? (GC stutter)
- [ ] Dados (cena/skins) data-driven em JSON, não hard-coded?
- [ ] Números mágicos centralizados? credenciais fora do repo?

## Referências (padrões de mercado)
- Game Programming Patterns — Robert Nystrom (gratuito): https://gameprogrammingpatterns.com/contents.html
- Three.js best practices (100 tips, 2026): https://www.utsubo.com/blog/threejs-best-practices-100-tips
- Prevenir memory leak no Three.js: https://roger-chi.vercel.app/blog/tips-on-preventing-memory-leak-in-threejs-scene
- ECS vs OOP (arquitetura/escala): https://www.daydreamsoft.com/blog/ecs-vs-oop-in-large-scale-games-choosing-the-right-architecture-for-performance-and-scalability
- Level up your code with game programming patterns (Unity): https://unity.com/blog/games/level-up-your-code-with-game-programming-patterns
