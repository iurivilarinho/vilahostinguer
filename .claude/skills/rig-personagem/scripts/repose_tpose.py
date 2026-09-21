# Da um esqueleto TEMPORARIO a uma malha SEM rig, repoe os bracos pra T-pose e ASSA
# a deformacao na malha (rest novo) -> sai uma malha em T-pose p/ o Mixamo rigar direito.
# Serve p/ modelos com pose ruim (braco-baixo, combate) sem esqueleto.
# uso: [ARM_T=1] blender -b -P repose_tpose.py -- entrada.glb saida.glb
import bpy, sys, os, math
from mathutils import Vector, Matrix
argv = sys.argv[sys.argv.index('--') + 1:]
src, dst = argv[0], argv[1]
ASCALE = float(os.environ.get('ARM_T', '1'))  # 1=T-pose horizontal; 0.5=A-pose

bpy.ops.wm.read_factory_settings(use_empty=True)
try: bpy.ops.import_scene.gltf(filepath=src, guess_original_bind_pose=False)
except TypeError: bpy.ops.import_scene.gltf(filepath=src)
for ob in list(bpy.data.objects):
    if ob.type == 'ARMATURE': bpy.data.objects.remove(ob, do_unlink=True)
for ob in bpy.data.objects:
    if ob.type == 'MESH' and ob.parent is not None:
        mw = ob.matrix_world.copy(); ob.parent = None; ob.matrix_world = mw
meshes = [o for o in bpy.data.objects if o.type == 'MESH']
if len(meshes) > 1:
    for o in bpy.data.objects: o.select_set(False)
    for o in meshes: o.select_set(True)
    bpy.context.view_layer.objects.active = meshes[0]
    bpy.ops.object.join()
mesh = [o for o in bpy.data.objects if o.type == 'MESH'][0]
# aplica transform pra trabalhar em world simples
bpy.context.view_layer.objects.active = mesh
mesh.select_set(True)
bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

# bbox + eixos (modelo gltf importado no Blender = Z-up; conferimos)
co = [v.co for v in mesh.data.vertices]
xs = [p.x for p in co]; ys = [p.y for p in co]; zs = [p.z for p in co]
size = [max(xs)-min(xs), max(ys)-min(ys), max(zs)-min(zs)]
UP = size.index(max(size)); rem = [i for i in range(3) if i != UP]
SIDE = rem[0] if size[rem[0]] >= size[rem[1]] else rem[1]
FWD = rem[1] if SIDE == rem[0] else rem[0]
arr = [xs, ys, zs]
umin, umax = min(arr[UP]), max(arr[UP]); H = umax - umin
def med(a):
    b = sorted(a); return b[len(b)//2]
cs = med(arr[SIDE]); cf = med(arr[FWD])
sh_u = umin + 0.80*H; hip_u = umin + 0.50*H
band = [abs(arr[SIDE][i]-cs) for i in range(len(co)) if hip_u <= arr[UP][i] <= sh_u and abs(arr[SIDE][i]-cs) < 0.25*H]
band.sort(); torso = min(band[int(len(band)*0.7)] if band else 0.12*H, 0.16*H)
armlen = 0.40*H; leglen = hip_u - umin
print('UP',UP,'SIDE',SIDE,'FWD',FWD,'H',round(H,3),'torso',round(torso,3))

def V(s, u, f=None):
    v = [0,0,0]; v[SIDE]=s; v[UP]=u; v[FWD]= cf if f is None else f
    return Vector(v)

# cria armadura com bracos PRA BAIXO (casa a pose hang) + pernas
bpy.ops.object.armature_add(enter_editmode=True, location=(0,0,0))
arm = bpy.context.object; arm.name='TmpRig'
eb = arm.data.edit_bones
for b in list(eb): eb.remove(b)
def bone(name, head, tail, parent=None):
    b = eb.new(name); b.head = head; b.tail = tail
    if parent: b.parent = parent; b.use_connect = False
    return b
spine = bone('spine', V(cs,hip_u), V(cs,sh_u))
for sgn,L in ((+1,'L'),(-1,'R')):
    sx = cs + sgn*torso
    ua = bone('upper'+L, V(sx,sh_u), V(sx,sh_u-armlen*0.5), spine)
    fa = bone('fore'+L, V(sx,sh_u-armlen*0.5), V(sx,sh_u-armlen), ua)
    lx = cs + sgn*torso*0.6
    ul = bone('upleg'+L, V(lx,hip_u), V(lx,hip_u-leglen*0.5), spine)
    ll = bone('loleg'+L, V(lx,hip_u-leglen*0.5), V(lx,umin), ul)
bpy.ops.object.mode_set(mode='OBJECT')

# skin: pesos MANUAIS por regiao (braco inteiro -> osso do ombro, faixa de transicao
# fina no ombro p/ nao rasgar). Mais forte que automatic weights p/ braco colado.
mesh.parent = arm
amod = mesh.modifiers.new('Armature', 'ARMATURE'); amod.object = arm
for nm in ['spine','upperL','foreL','upperR','foreR','uplegL','lolegL','uplegR','lolegR']:
    if nm not in mesh.vertex_groups: mesh.vertex_groups.new(name=nm)
vgU = {'L': mesh.vertex_groups['upperL'], 'R': mesh.vertex_groups['upperR']}
vgS = mesh.vertex_groups['spine']
for v in mesh.data.vertices:
    s = v.co[SIDE] - cs; u = v.co[UP]; asd = abs(s)
    inband = (hip_u - 0.12*H) <= u <= (sh_u + 0.06*H)
    # peso do braco: 0 no tronco -> 1 no braco. faixa LARGA = transicao gradual = menos rasgo.
    lo = float(os.environ.get('BAND_LO', '0.55')); hi = float(os.environ.get('BAND_HI', '1.5'))
    w = (asd - torso*lo) / (torso*(hi-lo))
    w = max(0.0, min(1.0, w)); w = w*w*(3-2*w)  # smoothstep
    if inband and w > 0:
        side = 'L' if s > 0 else 'R'
        vgU[side].add([v.index], w, 'REPLACE')
        vgS.add([v.index], 1.0 - w, 'REPLACE')
    else:
        vgS.add([v.index], 1.0, 'REPLACE')

# pose: gira upperarm pra apontar +/-SIDE (horizontal=T). filho (fore) acompanha.
bpy.ops.object.mode_set(mode='POSE')
for sgn,L in ((+1,'L'),(-1,'R')):
    pb = arm.pose.bones['upper'+L]
    # direcao alvo: lateral (T) misturada com baixo (A) conforme ASCALE
    d = [0,0,0]; d[SIDE]=sgn*ASCALE; d[UP]=-(1.0-ASCALE)
    dirv = Vector(d).normalized()
    head = arm.matrix_world @ pb.head
    quat = dirv.to_track_quat('Y','Z')
    pb.matrix = Matrix.Translation(head) @ quat.to_matrix().to_4x4()
    bpy.context.view_layer.update()
bpy.ops.object.mode_set(mode='OBJECT')

# assa: aplica o modifier de armadura na malha (congela a deformacao como novo rest)
for o in bpy.data.objects: o.select_set(False)
mesh.select_set(True); bpy.context.view_layer.objects.active = mesh
for md in list(mesh.modifiers):
    if md.type == 'ARMATURE':
        try: bpy.ops.object.modifier_apply(modifier=md.name)
        except Exception as e: print('apply fail', e)
bpy.data.objects.remove(arm, do_unlink=True)
bpy.ops.export_scene.gltf(filepath=dst, export_format='GLB', export_yup=True)
print('OK', os.path.getsize(dst))
