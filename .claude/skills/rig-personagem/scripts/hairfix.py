import bpy, sys, os
# re-pesa os vértices do material "Hair" 100% no osso da CABEÇA, pra o cabelo
# longo se mover rígido com a cabeça (sem esticar/soltar no corpo após o auto-rig).
a = sys.argv[sys.argv.index('--') + 1:]
src, dst = a[0], a[1]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=src)
fixed = 0
for obj in bpy.data.objects:
    if obj.type != 'MESH':
        continue
    head_vg = None
    for vg in obj.vertex_groups:
        if 'head' in vg.name.lower():
            head_vg = vg; break
    if not head_vg:
        continue
    hair_idx = set(i for i, ms in enumerate(obj.material_slots)
                   if ms.material and 'hair' in ms.material.name.lower())
    if not hair_idx:
        continue
    verts = set()
    for p in obj.data.polygons:
        if p.material_index in hair_idx:
            for v in p.vertices:
                verts.add(v)
    for vi in verts:
        for vg in obj.vertex_groups:
            try: vg.remove([vi])
            except Exception: pass
        head_vg.add([vi], 1.0, 'REPLACE')
    fixed += len(verts)
    print('reweighted', len(verts), 'hair verts on', obj.name, '-> head', head_vg.name)
print('total fixed', fixed)
bpy.ops.export_scene.gltf(filepath=dst, export_format='GLB', export_animations=False,
                          export_skins=True, export_yup=True)
print('OK', os.path.getsize(dst))
