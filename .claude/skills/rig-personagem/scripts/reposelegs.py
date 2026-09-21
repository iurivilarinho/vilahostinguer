import bpy, sys, math, mathutils, os
argv=sys.argv[sys.argv.index('--')+1:]
src, dst = argv[0], argv[1]
SPREAD=math.radians(float(os.environ.get('SPREAD','10')))  # abducao das coxas (graus)
bpy.ops.wm.read_factory_settings(use_empty=True)
ext=src.lower().rsplit('.',1)[-1]
if ext=='glb': bpy.ops.import_scene.gltf(filepath=src)
else: bpy.ops.import_scene.fbx(filepath=src)
arm=[o for o in bpy.data.objects if o.type=='ARMATURE'][0]
# detecta UP e lateral pela bbox
mn=[1e9]*3; mx=[-1e9]*3
for ob in bpy.data.objects:
    if ob.type=='MESH':
        for v in ob.data.vertices:
            w=ob.matrix_world@v.co
            for i in range(3): mn[i]=min(mn[i],w[i]); mx[i]=max(mx[i],w[i])
ext3=[mx[i]-mn[i] for i in range(3)]; up_i=ext3.index(max(ext3))
UP=mathutils.Vector((0,0,0)); UP[up_i]=1.0
fwd_i=ext3.index(min(ext3)); lat=[0,1,2]; lat.remove(up_i); lat.remove(fwd_i); li=lat[0]
bpy.context.view_layer.objects.active=arm; bpy.ops.object.mode_set(mode='POSE')
def spread_leg(name):
    pb=arm.pose.bones.get(name)
    if not pb: print('NO BONE',name); return
    M=arm.matrix_world@pb.matrix; head=M.to_translation(); tail=M@mathutils.Vector((0,pb.length,0))
    D=(tail-head).normalized()
    s=1.0 if head[li]>=0 else -1.0
    out=mathutils.Vector((0,0,0)); out[li]=s
    target=(-UP*math.cos(SPREAD)+out*math.sin(SPREAD)).normalized()
    q=D.rotation_difference(target); R=q.to_matrix().to_4x4()
    Mw=mathutils.Matrix.Translation(head)@R@mathutils.Matrix.Translation(-head)
    pb.matrix=Mw@pb.matrix; bpy.context.view_layer.update()
spread_leg('mixamorig:LeftUpLeg'); spread_leg('mixamorig:RightUpLeg')
bpy.context.view_layer.update(); bpy.ops.object.mode_set(mode='OBJECT')
for ob in [o for o in bpy.data.objects if o.type=='MESH']:
    am=[m for m in ob.modifiers if m.type=='ARMATURE']
    if not am: continue
    bpy.context.view_layer.objects.active=ob
    cp=ob.modifiers.new('bake','ARMATURE'); cp.object=am[0].object
    bpy.ops.object.modifier_apply(modifier=cp.name)
bpy.context.view_layer.objects.active=arm; bpy.ops.object.mode_set(mode='POSE'); bpy.ops.pose.armature_apply(); bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.export_scene.gltf(filepath=dst, export_format='GLB')
print('OK legs spread', os.path.basename(dst))
