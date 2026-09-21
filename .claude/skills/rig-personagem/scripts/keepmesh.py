# Separa personagens fundidos: MANTEM so as malhas listadas (remove o resto),
# preservando a armadura/skinning. P/ separar humano de shikigami, ou tirar arma.
# uso: blender -b -P keepmesh.py -- entrada.glb saida.glb Object_63,Object_65,Object_67
import bpy, sys, os
argv = sys.argv[sys.argv.index('--') + 1:]
src, dst, keep = argv[0], argv[1], argv[2]
keep_names = [k.strip() for k in keep.split(',') if k.strip()]
bpy.ops.wm.read_factory_settings(use_empty=True)
try: bpy.ops.import_scene.gltf(filepath=src, guess_original_bind_pose=False)
except TypeError: bpy.ops.import_scene.gltf(filepath=src)
removed = []
for ob in list(bpy.data.objects):
    if ob.type == 'MESH' and not any(k in ob.name for k in keep_names):
        removed.append(ob.name); bpy.data.objects.remove(ob, do_unlink=True)
print('removidas:', removed)
print('mantidas:', [o.name for o in bpy.data.objects if o.type == 'MESH'])
# remove ossos orfaos? nao — manter a armadura inteira (ossos extras nao atrapalham).
bpy.ops.export_scene.gltf(filepath=dst, export_format='GLB', export_skins=True, export_yup=True)
print('OK', os.path.getsize(dst))
