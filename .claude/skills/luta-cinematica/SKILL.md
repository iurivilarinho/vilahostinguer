---
name: luta-cinematica
description: Regras e técnica para a cinemática de LUTA do Convergence (ex.: Gojo × Sukuna) no Unity — roteiro fiel, combate que conecta, câmera com ênfase/chase (Cinemachine), poderes e cortes bonitos (VFX Graph), domínios, ambiente. Use sempre que mexer numa luta cinematográfica. Roteiro base em docs/fight-script.md.
---

# Luta cinematográfica — Convergence (Unity)

A luta é uma **Timeline** que orquestra animações Humanoid, **VFX Graph** (poderes/cortes)
e **Cinemachine** (câmera). O roteiro fiel e testado está em **`docs/fight-script.md`**
(beats com tempo + ação). Sempre **rodar a Timeline e OLHAR** (Game view / Unity Recorder,
vários ângulos) antes de dar como pronto.

## REGRAS DO DONO (não violar — cada uma veio de feedback)
1. **Roteiro FIEL e pesquisado.** Seguir `docs/fight-script.md` na ordem. Não inventar.
   Pesquisar a sequência real (web; fandom dá 403 → usar geekyinc/sportskeeda/gamerant).
2. **Poderes SAEM DAS MÃOS** — emitir o VFX a partir do bone da mão (Humanoid `RightHand`).
3. **TODO poder CONECTA:** orbe na mão → feixe → impacto no alvo → reação/knockback.
   Nunca um poder que não afeta ninguém.
4. **Combate de verdade:** atacante avança, bate, defensor REAGE (trocação/bloqueio/esquiva/
   nocaute). Quem é arremessado voa LONGE, **em pé** (clip hit, não death no meio da luta).
5. **CORPOS SÓLIDOS:** Colliders + resolução por frame — não se atravessam, nem às
   construções (o santuário do domínio é sólido).
6. **Facing DINÂMICO:** sempre encaram o oponente.
7. **Não "correr parado":** ao chegar no alvo, troca p/ a postura de combate.
8. **CIDADE DE DIA:** céu claro, prédios claros, sol + ambiente. Nada de cena escura.
   Tonemapping ACES no Volume da URP.
9. **CÂMERA com discernimento (Cinemachine):**
   - **Foca o PAR que luta** (Gojo×Mahoraga → não ficar no Sukuna parado).
   - **Punch-in estilo Mortal Kombat** nos golpes MARCANTES (Roxo, voto, invocação, final).
   - **Chase**: depois que o golpe conecta, seguir quem foi ARREMESSADO.
   - Implementar com vcams + Blends + **Cinemachine Impulse** (shake no impacto).
10. **CORTES finos e nítidos** (VFX Graph): traço de lâmina (núcleo branco + borda
    colorida), em rajada (Dismantle), e **barragem** = enxurrada de talhos. Feixes/orbes
    em CAMADAS (núcleo + halo + cabeça luminosa + faíscas), nunca primitiva chapada.
11. **DOMÍNIOS** nascem ATRÁS de cada lutador e crescem até o CENTRO. Sukuna = modelo
    `Malevolent Shrine` (`domain_shrine.glb`) + atmosfera vermelha; **Sukuna fica no TOPO
    da construção**, à frente, encarando o Gojo. Unlimited Void do Gojo = procedural
    (esfera estrelada). Gesto de mão antes.
12. **Domínios e shikigami COLAPSAM por DESINTEGRAÇÃO** (fragmentos que voam/caem +
    faíscas), nunca encolhendo até sumir. (VFX Graph point-cache do mesh.)
13. **GOLPE FINAL ÉPICO** ("o corte que corta mundos"): traço gigante + fenda + flash,
    câmera dramática + chase no Gojo caindo. Não pode ser genérico.
14. A luta é LONGA (vários rounds), ~114s; cada técnica marcante tem ênfase de câmera.
15. **Trocação rápida/snappy** (no protótipo era timeScale 1.65× nos golpes).

## Peculiaridades já resolvidas (do protótipo)
- **Mahoraga sem textura (branco):** o glb tem materiais sem map → no Unity, atribuir
  materiais URP por região (hakama, corpo, roda/chifres) ou texturizar.
- **Roda do Mahoraga** gira **como volante** (no plano dela), não rolando.
- **Cores estouradas:** era luz demais → equilibrar + ACES.
- **3 atores se sobrepondo:** posicionar explícito + colisão.

## Limites honestos (dizer ao dono)
- Não dá pra assistir vídeo quadro-a-quadro — roteiro pela sequência pesquisada (texto).
- Corpos de golpe usam clips Mixamo genéricos; a fidelidade está no roteiro, VFX, reações,
  câmera e cenário. Pra um golpe específico, baixar clip dedicado do Mixamo.

## Mapa de implementação (Unity)
- `beats[]` (protótipo) → **Timeline** com Signals/markers por beat.
- `power/strike/exchange` → animação Humanoid + VFX Graph disparado por Signal.
- `domainClash/giantPurple/finalCut/barrage` → sub-Timelines.
- `updateCam` (CLOSE/WIDE/DOMWIDE, special, chase) → **Cinemachine**.
- `disintegrate()` → VFX Graph (point-cache) ou ParticleSystem.

Ver [[reforma-engine-migracao]] e as skills [[jogo-personagem]], [[rig-personagem]].
Roteiro: `docs/fight-script.md`.
