import bpy, sys, os
import re
argv = sys.argv[sys.argv.index('--') + 1:]
src, dst = argv[0], argv[1]
EXCL = argv[2] if len(argv) > 2 else None  # regex de nomes de malha a remover
# malhas de efeito/fundo que estragam o upload (esfera de aura, ceu, etc.) -> sempre dropa.
# (a "bola entre os pes" do vegeta/muzan era uma Icosphere.) MX_KEEPFX desliga.
DEFAULT_DROP = r'icosphere|sphere|sky(box|dome|sphere)?|\baura\b|\borb\b|\bglow\b|vfx|energy|particle|backdrop|halo|ring'
bpy.ops.wm.read_factory_settings(use_empty=True)
# MX_NOGUESS: nao tenta "guess_original_bind_pose" (alguns modelos T-pose ja em pe
# ficam DEITADOS/virados com o guess, ex.: dbfz_majin_vegeta).
GUESS = not os.environ.get('MX_NOGUESS')
try:
    bpy.ops.import_scene.gltf(filepath=src, guess_original_bind_pose=GUESS)
except TypeError:
    bpy.ops.import_scene.gltf(filepath=src)
# remove armaduras (Mixamo re-rigga) e malhas excluídas (armas, efeitos, etc.)
had_armature = any(ob.type == 'ARMATURE' for ob in bpy.data.objects)
dropfx = (not os.environ.get('MX_KEEPFX'))
# antes de remover a armadura, "aplica" a pose atual (rest) na malha p/ as malhas
# skinadas não colapsarem ao perder o parent/armadura.
for ob in list(bpy.data.objects):
    if ob.type == 'ARMATURE':
        bpy.data.objects.remove(ob, do_unlink=True)
    elif ob.type == 'MESH' and EXCL and re.search(EXCL, ob.name, re.I):
        print('drop mesh', ob.name)
        bpy.data.objects.remove(ob, do_unlink=True)
    elif ob.type == 'MESH' and dropfx and re.search(DEFAULT_DROP, ob.name, re.I):
        print('drop fx mesh', ob.name)
        bpy.data.objects.remove(ob, do_unlink=True)
# remove modificadores de armadura órfãos
for ob in bpy.data.objects:
    if ob.type == 'MESH':
        for md in list(ob.modifiers):
            if md.type == 'ARMATURE':
                ob.modifiers.remove(md)
# LIMPA PARENTS (mantendo transform mundial): as rotacoes abaixo so atuam em malhas
# parentless (ob.parent is None). Modelos com malhas parenteadas a empty/node ficavam
# SEM girar -> orientacao errada (de perfil): obiwan, ravena. Agora vira tudo top-level.
for ob in bpy.data.objects:
    if ob.type == 'MESH' and ob.parent is not None:
        mw = ob.matrix_world.copy()
        ob.parent = None
        ob.matrix_world = mw
# JOIN: junta todas as malhas numa só (auto-rigger do Mixamo é mais confiável com
# 1 malha). MAS NÃO junta se o modelo tinha esqueleto — juntar malhas skinadas
# colapsa pra uma "esfera" (bug do muzan/toji). Skinados: mantém malhas separadas.
if os.environ.get('MX_JOIN') and not had_armature:
    meshes = [o for o in bpy.data.objects if o.type == 'MESH']
    if len(meshes) > 1:
        for o in bpy.data.objects:
            o.select_set(False)
        for o in meshes:
            o.select_set(True)
        bpy.context.view_layer.objects.active = meshes[0]
        bpy.ops.object.join()
        print('JOINED', len(meshes), 'meshes')
# bbox ROBUSTO (percentil 2-98 dos vértices reais) -> ignora geometria perdida
import mathutils
xs, ys, zs = [], [], []
nmesh = 0; narm = 0
for ob in bpy.data.objects:
    if ob.type == 'MESH':
        nmesh += 1
        mw = ob.matrix_world
        for v in ob.data.vertices:
            w = mw @ v.co
            xs.append(w.x); ys.append(w.y); zs.append(w.z)
    if ob.type == 'ARMATURE':
        narm += 1
def pct(arr, p):
    arr = sorted(arr); i = max(0, min(len(arr) - 1, int(p * (len(arr) - 1))))
    return arr[i]
def iqr_bounds(arr):  # extensão dos INLIERS (regra IQR) — ignora lixo grande/longe
    a = sorted(arr); n = len(a)
    q1 = a[max(0, int(0.25 * (n - 1)))]; q3 = a[max(0, int(0.75 * (n - 1)))]
    iqr = q3 - q1; lo = q1 - 1.5 * iqr; hi = q3 + 1.5 * iqr
    inl = [v for v in a if lo <= v <= hi] or a
    return inl[0], inl[-1]
xlo, xhi = iqr_bounds(xs); ylo, yhi = iqr_bounds(ys); zlo, zhi = iqr_bounds(zs)
mn = mathutils.Vector((xlo, ylo, zlo))
mx = mathutils.Vector((xhi, yhi, zhi))
size = mx - mn
print('BBOX(p) x=%.3f y=%.3f z=%.3f  meshes=%d arms=%d verts=%d' % (size.x, size.y, size.z, nmesh, narm, len(zs)))
# AUTO-ENDIREITA: o corpo (eixo mais LONGO) deve ficar vertical (Z). Se estiver
# deitado (toji: longo em Y), rotaciona; depois recalcula o bbox.
import math
dims = {'x': size.x, 'y': size.y, 'z': size.z}
longest = max(dims, key=dims.get)
Rax = {'y': 'X', 'x': 'Y'}.get(longest)
if Rax:
    Rm = mathutils.Matrix.Rotation(math.radians(90), 4, Rax)
    for ob in bpy.data.objects:
        if ob.type == 'MESH' and ob.parent is None:
            ob.matrix_world = Rm @ ob.matrix_world
    xs2, ys2, zs2 = [], [], []
    for ob in bpy.data.objects:
        if ob.type == 'MESH':
            mw = ob.matrix_world
            for v in ob.data.vertices:
                w = mw @ v.co; xs2.append(w.x); ys2.append(w.y); zs2.append(w.z)
    xs, ys, zs = xs2, ys2, zs2
    xlo, xhi = iqr_bounds(xs); ylo, yhi = iqr_bounds(ys); zlo, zhi = iqr_bounds(zs)
    mn = mathutils.Vector((xlo, ylo, zlo)); mx = mathutils.Vector((xhi, yhi, zhi))
    size = mx - mn
    print('UPRIGHT rot %s -> x=%.3f y=%.3f z=%.3f' % (Rax, size.x, size.y, size.z))
# ENCARAR A FRENTE: a LARGURA (ombros) deve ficar no X; a profundidade no Y.
# Se a largura estiver no Y (modelo de perfil pro Mixamo, ex.: ravena), rotaciona
# 90° no Z. (largura > profundidade pra humanos). MX_NOFACE pula.
if not os.environ.get('MX_NOFACE') and size.y > size.x * 1.15:
    Rz = mathutils.Matrix.Rotation(math.radians(90), 4, 'Z')
    for ob in bpy.data.objects:
        if ob.type == 'MESH' and ob.parent is None:
            ob.matrix_world = Rz @ ob.matrix_world
    xs3, ys3, zs3 = [], [], []
    for ob in bpy.data.objects:
        if ob.type == 'MESH':
            mw = ob.matrix_world
            for v in ob.data.vertices:
                w = mw @ v.co; xs3.append(w.x); ys3.append(w.y); zs3.append(w.z)
    xs, ys, zs = xs3, ys3, zs3
    xlo, xhi = iqr_bounds(xs); ylo, yhi = iqr_bounds(ys); zlo, zhi = iqr_bounds(zs)
    mn = mathutils.Vector((xlo, ylo, zlo)); mx = mathutils.Vector((xhi, yhi, zhi))
    size = mx - mn
    print('FACE rot Z -> x=%.3f y=%.3f z=%.3f' % (size.x, size.y, size.z))
# GIRO MANUAL (graus no Z): override fino de orientacao quando a heuristica erra
# (modelo encarando o lado errado). ex.: MX_YAW=90 ou MX_YAW=-90.
YAW = float(os.environ.get('MX_YAW', '0') or 0)
if abs(YAW) > 0.001:
    Rz = mathutils.Matrix.Rotation(math.radians(YAW), 4, 'Z')
    for ob in bpy.data.objects:
        if ob.type == 'MESH' and ob.parent is None:
            ob.matrix_world = Rz @ ob.matrix_world
    xs4, ys4, zs4 = [], [], []
    for ob in bpy.data.objects:
        if ob.type == 'MESH':
            mw = ob.matrix_world
            for v in ob.data.vertices:
                w = mw @ v.co; xs4.append(w.x); ys4.append(w.y); zs4.append(w.z)
    xs, ys, zs = xs4, ys4, zs4
    xlo, xhi = iqr_bounds(xs); ylo, yhi = iqr_bounds(ys); zlo, zhi = iqr_bounds(zs)
    mn = mathutils.Vector((xlo, ylo, zlo)); mx = mathutils.Vector((xhi, yhi, zhi))
    size = mx - mn
    print('YAW %g -> x=%.3f y=%.3f z=%.3f' % (YAW, size.x, size.y, size.z))
# NORMALIZA: altura (Z, pois Blender é Z-up) -> 1.7m, pés em z=0, centrado em x/y.
h = size.z if size.z > 1e-9 else 1.0
s = 1.7 / h
center = mathutils.Vector(((mn.x + mx.x) / 2.0, (mn.y + mx.y) / 2.0, mn.z))
M = mathutils.Matrix.Scale(s, 4) @ mathutils.Matrix.Translation(-center)
for ob in bpy.data.objects:
    if ob.type == 'MESH' and ob.parent is None:
        ob.matrix_world = M @ ob.matrix_world
print('NORM s=%.4f -> height ~1.7' % s)
# LEAN: decima e tira IMAGENS (modelos pesados travam o auto-rigger; a textura
# é reaplicada depois pelo retex.py a partir do GLB original).
# IMPORTANTE: MANTÉM os SLOTS de material (só remove as imagens pesadas) — antes
# fazia materials.clear() e o mesh riggado ficava SEM slots, então o retex não tinha
# onde aplicar e o personagem saía BRANCO (choso/roupa toon-shaded). Mantendo os
# slots+nomes, o retex casa por nome e a roupa volta colorida.
LEAN = os.environ.get('MX_LEAN')
# CAP de verts POR MALHA (default 12000). Modelos com MUITAS malhas (choso, 26 malhas
# ~195k verts) travam o rigger mesmo com cada malha <12k -> baixar MX_DECVERTS p/ caber.
DECVERTS = float(os.environ.get('MX_DECVERTS', '12000'))
if LEAN:
    for ob in bpy.data.objects:
        if ob.type == 'MESH':
            nv = len(ob.data.vertices)
            if nv > DECVERTS:
                md = ob.modifiers.new('dec', 'DECIMATE'); md.ratio = max(0.08, DECVERTS / nv)
    for img in list(bpy.data.images):
        try: bpy.data.images.remove(img)
        except Exception: pass
# exporta FBX (binário). inclui malha; sem animação.
bpy.ops.export_scene.fbx(filepath=dst, use_selection=False, add_leaf_bones=False,
                         path_mode=('AUTO' if LEAN else 'COPY'),
                         embed_textures=(not LEAN), mesh_smooth_type='FACE',
                         use_mesh_modifiers=True)
print('OK', os.path.basename(dst), os.path.getsize(dst))
