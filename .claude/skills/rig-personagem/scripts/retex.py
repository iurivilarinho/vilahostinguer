import bpy, sys, os
# reaplica os materiais TEXTURIZADOS do GLB original no mesh RIGGADO (FBX),
# casando por nome de material (object_0/1/2). Mantém esqueleto + texturas.
a = sys.argv[sys.argv.index('--') + 1:]
rig_fbx, orig_glb, out_glb = a[0], a[1], a[2]
bpy.ops.wm.read_factory_settings(use_empty=True)
# 1) importa o riggado (esqueleto + mesh skinada, materiais sem textura)
bpy.ops.import_scene.fbx(filepath=rig_fbx, automatic_bone_orientation=False)
rigged_meshes = [o for o in bpy.data.objects if o.type == 'MESH']
armatures = [o for o in bpy.data.objects if o.type == 'ARMATURE']
# 2) importa o original (mesh + materiais texturizados)
before = set(bpy.data.objects)
bpy.ops.import_scene.gltf(filepath=orig_glb)
orig_objs = [o for o in bpy.data.objects if o not in before]
orig_meshes = [o for o in orig_objs if o.type == 'MESH']
# mapa nome-base -> material texturizado do original
def base(n): return n.split('.')[0]
orig_mats = {}
for om in orig_meshes:
    for slot in om.material_slots:
        if slot.material:
            orig_mats[base(slot.material.name)] = slot.material
orig_list = []  # ordem dos materiais do original (p/ fallback posicional)
for om in orig_meshes:
    for slot in om.material_slots:
        if slot.material and slot.material not in orig_list:
            orig_list.append(slot.material)
print('orig mats:', list(orig_mats.keys()))
# 3) reaplica nos meshes riggados: 1º casa por NOME; se não casar, por ORDEM (índice)
for rm in rigged_meshes:
    for i, slot in enumerate(rm.material_slots):
        key = base(slot.material.name) if slot.material else None
        if key and key in orig_mats:
            slot.material = orig_mats[key]; print('  set', rm.name, 'slot', i, '-> name', key)
        elif i < len(orig_list):
            slot.material = orig_list[i]; print('  set', rm.name, 'slot', i, '-> idx', orig_list[i].name)
# 4) remove os objetos do original (mantém só riggado + materiais reaproveitados)
for o in orig_objs:
    bpy.data.objects.remove(o, do_unlink=True)
# 4b) dropa malhas de efeito/fundo (icosphere/esfera/aura...) que sobram no mesh
# RIGGADO — o Mixamo às vezes mantém a esfera, e ela vira "bola" no app.
import re as _re
_DROP = r'icosphere|sphere|sky(box|dome)?|\baura\b|\borb\b|\bglow\b|vfx|halo'
for o in list(bpy.data.objects):
    if o.type != 'MESH':
        continue
    nm = o.name + '|' + (o.data.name if o.data else '')   # checa nome do OBJ e do MESH-DATA
    nv = len(o.data.vertices) if o.data else 0
    if _re.search(_DROP, nm, _re.I) or nv == 42:           # 42 verts = icosphere default do Blender
        print('drop fx', o.name, '/', o.data.name if o.data else '-', 'v=' + str(nv))
        bpy.data.objects.remove(o, do_unlink=True)
# 4c) MATA o widget de osso do Mixamo (icosphere de display): limpa custom_shape de
# TODAS as pose bones e remove a mesh-data "Icosphere" orfã — o exportador glTF
# inclui essa malha senão, virando uma "bola" no app.
for arm in [o for o in bpy.data.objects if o.type == 'ARMATURE']:
    for pb in arm.pose.bones:
        pb.custom_shape = None
for m in list(bpy.data.meshes):
    if _re.search(r'icosphere|sphere', m.name, _re.I) or len(m.vertices) == 42:
        try:
            print('drop mesh-data', m.name); bpy.data.meshes.remove(m)
        except Exception:
            pass
# 5) exporta GLB
bpy.ops.export_scene.gltf(filepath=out_glb, export_format='GLB', export_animations=False, export_skins=True, export_yup=True)
print('OK', os.path.getsize(out_glb))
