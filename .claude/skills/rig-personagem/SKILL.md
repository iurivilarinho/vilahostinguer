---
name: rig-personagem
description: Adicionar/consertar personagens no Convergence (Unity). Auto-rig no Mixamo p/ modelos SEM esqueleto, separar armas/shikigami, retexturar — e depois importar no Unity como Humanoid (o retarget é NATIVO, não há mais retarget.js). Use ao adicionar um modelo novo, dar esqueleto a um sem rig, ou baixar animações do Mixamo. Scripts em ./scripts/.
---

# Rig de personagem — Convergence (Unity 6.1)

> **Mudança-chave vs. o protótipo Three.js:** no Unity o retarget é **nativo (Humanoid)**.
> Some todo o `retarget.js`/`bonemap.js`/`stepFlip`/`reposelegs`. Você ainda usa o
> **Mixamo** só p/ DAR ESQUELETO a modelos que não têm (Unity não rigga mesh cru).
> Depois é importar o `.glb`/`.fbx` e marcar **Rig → Animation Type: Humanoid**.

Modelos em `Assets/Art/Characters/*.glb`, fichas em `Assets/Data/`. Os scripts de
Mixamo/Blender rodam FORA do Unity (Node/puppeteer + Blender 5.1).

## CREDENCIAIS Mixamo / Adobe (o dono compartilha aberto)
- Email: **mxtreze@gmail.com** · Senha: **@40028922Ab** (mesmo login do Sketchfab)
- Char-fonte do auto-rig: **"Adam"** (`d8d42014-7b46-4784-9803-9eb95f197233`).

---

## Fluxo no Unity (modelo que JÁ tem rig Mixamo)
1. Copie o `.glb` p/ `Assets/Art/Characters/`.
2. No Inspector do modelo → aba **Rig** → **Animation Type: Humanoid** → **Apply** →
   **Configure...** e confira o mapeamento de ossos (verde = ok).
3. Crie/atribua o **Animator Controller** compartilhado. Como é Humanoid, **as mesmas
   animações Mixamo retargam pra QUALQUER personagem** — é o ganho central do Unity.
4. Materiais: a aba **Materials** → Extract Materials (ajustar shader URP se vier sem cor).

## Fluxo p/ modelo SEM esqueleto (auto-rig no Mixamo) — continua valendo
A separação de armas/shikigami e o auto-rig são iguais ao protótipo (conhecimento caro,
não repetir os bugs). Depois do FBX riggado, importa no Unity como Humanoid (acima).

### 0. SEPARAR armas e shikigami ANTES do Mixamo (OBRIGATÓRIO)
Remova do mesh toda arma (espada/adaga/lança/corrente) e todo shikigami/familiar.
Personagem e shikigami/arma são entidades DIFERENTES — fundir confunde os marcadores e
gera rig torto. Casos: **Toji** (espada), **Higuruma/Judgeman** (humano + shikigami = 2),
Toji_c (corrente). Ferramentas: `scripts/keepmesh.py <in> <out> Mesh1,Mesh2` (mantém só
as malhas, preserva rig), `scripts/split.py` (por prefixo de material), `scripts/meshpick.mjs`
(lista bbox/centróide p/ achar a arma). Arma/shikigami que o dono quer = rigar separado.

### 1. Triagem
`node scripts/inspect.mjs <id>`: rig limpo (biped/mixamo) → importa direto no Unity.
skins=0 ou rig ruim → auto-rig. Não-humanoide → pular.

### 2. Auto-rig no Mixamo
1. **FBX:** `MX_JOIN=1 [MX_LEAN=1] blender -b -P scripts/glb2fbx.py -- in.glb _t/_rig/<id>.fbx [excluir_regex]`
   - Normaliza (1.7m, pés no chão, encara a frente), bbox IQR, remove armaduras/esferas.
   - `MX_JOIN=1` só se NÃO houver esqueleto (juntar malha skinada vira BOLA).
   - `MX_LEAN=1` decima + remove imagens pesadas (>10MB travam o rigger) MAS mantém slots
     de material (senão a roupa volta branca; retextura casa por nome).
2. **Rigar:** `MX_EMAIL=.. MX_PASS=.. [MX_NOPLACE=1] MX_FILE=_t/_rig/<id>.fbx MX_OUT=<id> timeout 560 node scripts/mixamo_rig.mjs`
   - timeout **≥480s** (rig leva ~2min; curto MATA no fim e baixa o personagem ERRADO).
   - **Pose importa:** A/T-pose → ótimo rig. Pose de combate → use `MX_NOPLACE=1`.
   - **NUNCA 2 filas simultâneas** (mesma conta → navegadores brigam). Fila serial.
3. **Retexturar:** `blender -b -P scripts/retex.py -- _t/_rigged/<id>.fbx original.glb out.glb`
   (reaplica materiais do GLB original por nome; UVs preservados pelo Mixamo).
4. Importa o resultado no Unity como Humanoid.

### Modelo com 2 personagens juntos (Yuta+Rika)
`blender -b -P scripts/split.py -- junto.glb saida.glb MI_CP` (por prefixo de material),
1× por personagem, depois auto-rig cada.

---

## PECULIARIDADES já resolvidas (não repetir)
- **Esfera/bola no Mixamo (muzan/toji/mando):** malha "Icosphere" de efeito domina o
  upload → glb2fbx remove por nome (icosphere/sky/aura/orb/glow/vfx/particle). As bolas
  azuis na mão do gojo_shinjuku eram o mesmo caso (removidas por material).
- **JOIN de malha skinada vira bola** → glb2fbx pula join se houver armature.
- **Deitado de lado (muzan):** auto-endireitar só se eixo longo > Z em 1.25×.
- **Roupa branca pós-rig (choso/frieren):** `MX_LEAN` limpava materiais → manter slots;
  retex casa por nome. Modelo leve (<8MB): prep SEM LEAN (texturas embarcam).
- **Vader sem cor:** Mixamo descartou todos materiais → pintar sólido (`scripts/paint.py`).
- **Pintar roupa toon multi-cor (choso/fern):** `scripts/paint_bymat.py` + DROP das cascas
  de outline (senão cobre tudo de preto).
- **Cabelo/peça rígida esticando (nezuko):** `scripts/hairfix.py` re-pesa no osso Head.
- **Montar meio-corpo (fern):** enxertar pernas de outro (ver histórico `fern_graft.py`).
- **Moonwalk / pés cruzando / andar de costas:** NO UNITY ISSO SOME (retarget Humanoid
  resolve). Não portar `stepFlip`/`reposelegs`/heurística de adução — eram gambiarra da
  Three.js. Se um Humanoid ficar estranho, é o **Avatar mal mapeado** → Configure Avatar.
- **REGRA DE OURO:** ao consertar um, NÃO mexer em quem já funciona.

---

## CLIPS de animação (Mixamo → Humanoid)
Baixar via **API** (robusto): `MX_EMAIL=.. MX_PASS=.. MX_LIST='walk:walking:1,run:running:1,idle:idle:0,jump:jump:0' node scripts/mixamo_api.mjs`
(formato `arquivo:busca:inplace`; casa nome EXATO do produto). No Unity, importe o FBX da
animação como **Humanoid** e ele retarga p/ todos. Não precisa mais do `strip_glb.py`
(era p/ a Three.js), mas pode usar p/ inspecionar.

## TESTE
- **Play mode / Animation Preview** no Inspector do clip.
- **Unity Recorder** p/ capturar frames de vários ângulos (regra do dono: SEMPRE olhar).
- Cena de bench com vários personagens lado a lado rodando o mesmo clip.

Ver [[reforma-engine-migracao]], [[mixamo-autorig-pipeline]], [[mixamo-clip-pipeline]],
[[reforma-elenco-status]].
