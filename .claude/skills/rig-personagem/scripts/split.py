import bpy, sys, os
# separa um GLB com 2 personagens: mantém só as malhas cujo material começa com
# KEEP (prefixo), remove o resto. uso: -- src.glb dst.glb MI_CP
a = sys.argv[sys.argv.index('--') + 1:]
src, dst, keep = a[0], a[1], a[2]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=src)
removed = 0
for ob in list(bpy.data.objects):
    if ob.type != 'MESH':
        continue
    mats = [ms.material.name for ms in ob.material_slots if ms.material]
    keepit = any(mn.startswith(keep) for mn in mats)
    if not keepit:
        bpy.data.objects.remove(ob, do_unlink=True); removed += 1
print('removed', removed, 'kept material prefix', keep)
bpy.ops.export_scene.gltf(filepath=dst, export_format='GLB', export_animations=False, export_skins=False, export_yup=True)
print('OK', os.path.getsize(dst))
