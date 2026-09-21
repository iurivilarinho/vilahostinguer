import bpy, sys, os
argv = sys.argv[sys.argv.index('--') + 1:]
srcdir, dstdir = argv[0], argv[1]
os.makedirs(dstdir, exist_ok=True)
for f in sorted(os.listdir(srcdir)):
    if not f.endswith('.fbx'):
        continue
    base = f[:-4]
    try:
        bpy.ops.wm.read_factory_settings(use_empty=True)
        bpy.ops.import_scene.fbx(filepath=os.path.join(srcdir, f), automatic_bone_orientation=False)
        # decima a malha pesado (só precisamos do esqueleto+skin p/ ler ossos)
        for ob in list(bpy.data.objects):
            if ob.type == 'MESH':
                m = ob.modifiers.new('dec', 'DECIMATE')
                m.ratio = 0.03
                # limpa materiais (tira texturas)
                ob.data.materials.clear()
        # remove imagens da blend
        for img in list(bpy.data.images):
            try: bpy.data.images.remove(img)
            except Exception: pass
        bpy.ops.export_scene.gltf(filepath=os.path.join(dstdir, 'mx_' + base + '.glb'),
                                  export_format='GLB', export_animations=True,
                                  export_skins=True, export_yup=True,
                                  export_materials='NONE', export_image_format='NONE',
                                  export_apply=True)
        sz = os.path.getsize(os.path.join(dstdir, 'mx_' + base + '.glb'))
        print('OK', base, sz)
    except Exception as e:
        print('FAIL', base, e)
