# Conversao GLB -> FBX LIMPA p/ Unity Humanoid (preserva armature/skin/anim;
# SEM a normalizacao do auto-rig do Mixamo). Uso:
#   blender -b -P glb2fbx_clean.py -- entrada.glb saida.fbx
import bpy, sys

argv = sys.argv[sys.argv.index("--") + 1:]
inp, outp = argv[0], argv[1]
# 3o arg "anim" baka animacao (clipes mx_*); personagens devem vir SEM (bind limpo em pe)
bake = len(argv) > 2 and argv[2] == "anim"

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=inp)

bpy.ops.export_scene.fbx(
    filepath=outp,
    use_selection=False,
    apply_unit_scale=True,
    apply_scale_options='FBX_SCALE_ALL',
    bake_space_transform=False,
    add_leaf_bones=False,          # Unity Humanoid nao gosta de leaf bones
    primary_bone_axis='Y',
    secondary_bone_axis='X',
    use_armature_deform_only=False,
    bake_anim=bake,                # so baka p/ clipes (personagem = bind limpo, sem pose deitada)
    bake_anim_use_all_actions=bake,
    path_mode='COPY',
    embed_textures=True,
    mesh_smooth_type='FACE',
)
print("OK FBX ->", outp)
