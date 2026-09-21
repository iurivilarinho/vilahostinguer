---
name: jogo-personagem
description: Camada de JOGO do Convergence (Unity) — fichas de personagem (config+habilidades), ranking de poder, sets de animação por arquétipo, HUD de combate (vida/mana/golpes), menu de pausa (ESC), movelist (TAB, estilo Mortal Kombat) e demos. Tudo dirigido por dados (JSON → ScriptableObject). Use ao mexer em atributos, habilidades, tiers, telas ou demos. Para rigar modelos veja a skill rig-personagem.
---

# Camada de jogo — Convergence (Unity)

Sobre os personagens Humanoid (ver [[rig-personagem]]), esta camada dá "cara de game":
fichas, habilidades por tecla, ranking de poder, HUD, menus, demos. **Tudo dirigido por
dados** em `Assets/Data/` → carregados como **ScriptableObject** em runtime.

## A) Fichas (config + habilidades)
Os dados vieram do protótipo (`Assets/Data/units.json`, `units/<id>.json`, `archetypes.json`,
`index.json`). Cada ficha: universo, world, species, height_m, headgear, powerSources,
uses{magic,ki,force,chakra,cursedEnergy,bruteForce,demon}, powers[], signatureMove,
combatStyle, archetype, tier, powerLevel, **stats{hp,mana,attack,defense,speed}**,
**weaknesses[]**, **resistances[]**, canFly, weapons, abilities[].

No Unity: um `CharacterData : ScriptableObject` + um importador que gera 1 asset por ficha
a partir do JSON (ver `Assets/Scripts/Editor`). Manter o JSON como fonte facilita editar em
massa (o protótipo gerava via `_gen_units.mjs`).

**Ranking 0–100 cross-universe** (balanceamento, não canônico). Restrições FIXAS do dono:
**Vader < Omni-Man**, **Vader < Goku**, **Vader > Maul**, **Vader > Bolsonaro**.
**Fraquezas/resistências** são lore-reais (pesquisar p/ novas: viltrumita=frequência sonora;
saiyajin=cauda/esgotamento de ki; demônio DS=sol/Nichirin/glicínia; JJK=Mahoraga/Inverted
Spear/Domain Amplification).

## B) MOVESET INDIVIDUAL (5 golpes + assinatura por personagem)
Cada personagem tem **5 golpes próprios** (lore-fiéis), não genéricos por arquétipo
(Gojo: Blue/Red/Hollow Purple/Unlimited Void/Infinity ≠ Sukuna: Dismantle/Cleave/Spiderweb/
Fire Arrow/Malevolent Shrine). Teclas/botões via **Input System** (mapear p/ teclado +
DualSense). Cada golpe → um estado/clip no Animator + um VFX Graph.

## C) Telas (UI Toolkit)
- **Menu principal** "CONVERGENCE": jogar / escolher personagem / demo / luta cinematográfica.
- **ESC → Pausa:** Continuar / Golpes / Trocar / Sair.
- **TAB → Movelist** (estilo Mortal Kombat): atributos (barras), fraquezas, resistências,
  golpes + execução, poderes (lore). Pausa enquanto aberta.
- **HUD de combate:** retrato, vida, mana, contador de golpes, crosshair.
- Construir em **UXML/USS** (UI Toolkit). Badge de tier por classe (Sp/S/A/B/C/D/F).

## D) Demos / cinemática
A luta **Gojo × Sukuna** é a cinemática-carro-chefe → ver skill [[luta-cinematica]] e
`docs/fight-script.md`. Estrutura no Unity: Timeline + Cinemachine + VFX Graph.

## E) Input (PC + console)
**Input System** com Action Maps (Gameplay/UI). Suporta teclado/mouse e gamepad
(DualSense) sem código duplicado — essencial p/ o alvo PS5.

## F) TESTE
- **Play mode** + Unity Recorder (vários ângulos — regra do dono: SEMPRE olhar).
- Testes de dados: validar que cada ficha carrega no ScriptableObject e bate teclas→golpes.
- Cena de bench p/ movelist/HUD.

Ver [[reforma-fichas-habilidades]], [[reforma-elenco-status]], [[reforma-engine-migracao]].
