import bpy,sys,os,re
a=sys.argv[sys.argv.index('--')+1:]
src,dst,spec=a[0],a[1],a[2]
DROP=r'icosphere|sky(box|dome|sphere)?|\baura\b|\borb\b|\bglow\b'
rules=[]
for part in spec.split(';'):
    if not part.strip(): continue
    k,c=part.split(':'); r,g,b=[float(x) for x in c.split(',')]
    rules.append((k,(r,g,b)))
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=src)
def mkmat(name,rgb):
    m=bpy.data.materials.new(name); m.use_nodes=True
    nt=m.node_tree; bs=nt.nodes.get('Principled BSDF')
    bs.inputs['Metallic'].default_value=0.0; bs.inputs['Roughness'].default_value=0.6
    # textura sólida (8x8) — renderiza certo no app (baseColorFactor puro fica cinza)
    img=bpy.data.images.new(name+'_t',8,8); img.pixels=[rgb[0],rgb[1],rgb[2],1.0]*64
    tx=nt.nodes.new('ShaderNodeTexImage'); tx.image=img
    nt.links.new(tx.outputs['Color'],bs.inputs['Base Color'])
    return m
cache={}
for ob in [o for o in bpy.data.objects if o.type=='MESH' and re.search(DROP,o.name,re.I)]:
    print('drop',ob.name); bpy.data.objects.remove(ob,do_unlink=True)
for ob in [o for o in bpy.data.objects if o.type=='MESH']:
    rgb=None
    for k,c in rules:
        if re.search(k,ob.name,re.I): rgb=c; break
    if rgb is None: continue
    key=str(rgb); mat=cache.get(key) or cache.setdefault(key,mkmat('c'+str(len(cache)),rgb))
    ob.data.materials.clear(); ob.data.materials.append(mat); print('paint',ob.name,rgb)
bpy.ops.export_scene.gltf(filepath=dst,export_format='GLB',export_animations=False,export_skins=True,export_yup=True)
print('OK',os.path.getsize(dst))
