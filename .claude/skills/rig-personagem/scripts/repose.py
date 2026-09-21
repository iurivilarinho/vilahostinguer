import bpy, sys, os
# reimporta GLB reconstruindo a REST pose a partir das inverse-bind (T/A-pose),
# pra modelos que vêm com rest posada (toji, gojo_shinjuku). Reexporta GLB.
argv = sys.argv[sys.argv.index('--') + 1:]
src, dst = argv[0], argv[1]
NOCLEAR = '--noclear' in argv
bpy.ops.wm.read_factory_settings(use_empty=True)
try:
    bpy.ops.import_scene.gltf(filepath=src, guess_original_bind_pose=True)
except TypeError:
    bpy.ops.import_scene.gltf(filepath=src)
# garante que a pose atual = rest (limpa qualquer pose aplicada)
for ob in bpy.data.objects:
    if ob.type == 'ARMATURE' and not NOCLEAR:
        bpy.context.view_layer.objects.active = ob
        try:
            bpy.ops.object.mode_set(mode='POSE')
            bpy.ops.pose.select_all(action='SELECT')
            bpy.ops.pose.transforms_clear()  # zera pose -> volta pra rest (T-pose)
            bpy.ops.object.mode_set(mode='OBJECT')
        except Exception as e:
            print('pose clear fail', e)
bpy.ops.export_scene.gltf(filepath=dst, export_format='GLB', export_animations=True,
                          export_skins=True, export_yup=True)
print('OK', os.path.basename(dst), os.path.getsize(dst))
