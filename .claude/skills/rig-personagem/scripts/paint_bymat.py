import bpy,sys,os,re
a=sys.argv[sys.argv.index('--')+1:]
src,dst,spec=a[0],a[1],a[2]
# spec: "matkey:r,g,b;matkey:r,g,b;..."  (casa por base do nome do MATERIAL)
rules=[]
for part in spec.split(';'):
    if not part.strip(): continue
    k,c=part.split(':')
    if c.strip().upper()=='DROP': rules.append((k,'DROP')); continue
    r,g,b=[float(x) for x in c.split(',')]
    rules.append((k,(r,g,b)))
bpy.ops.wm.read_factory_settings(use_empty=True)
try: bpy.ops.import_scene.gltf(filepath=src,guess_original_bind_pose=False)
except TypeError: bpy.ops.import_scene.gltf(filepath=src)
def mkmat(name,rgb):
    m=bpy.data.materials.new(name); m.use_nodes=True
    nt=m.node_tree; bs=nt.nodes.get('Principled BSDF')
    bs.inputs['Metallic'].default_value=0.0; bs.inputs['Roughness'].default_value=0.6
    img=bpy.data.images.new(name+'_t',8,8); img.pixels=[rgb[0],rgb[1],rgb[2],1.0]*64
    tx=nt.nodes.new('ShaderNodeTexImage'); tx.image=img
    nt.links.new(tx.outputs['Color'],bs.inputs['Base Color'])
    return m
cache={}
def colfor(matname):
    base=matname.split('.')[0].lower()
    for k,c in rules:
        if k.lower() in base: return c
    return None
for ob in [o for o in bpy.data.objects if o.type=='MESH']:
    # icosphere/efeito fora
    if re.search(r'icosphere|sphere',(ob.data.name if ob.data else '')+ob.name,re.I) or len(ob.data.vertices)==42:
        print('drop',ob.name); bpy.data.objects.remove(ob,do_unlink=True); continue
    # casca de OUTLINE toon (inverted hull) -> REMOVE (senao cobre o corpo de preto)
    mnames=' '.join(m.name for m in ob.data.materials if m)
    if re.search(r'outline|OH_Outline',mnames,re.I):
        print('drop outline',ob.name); bpy.data.objects.remove(ob,do_unlink=True); continue
    # materiais marcados DROP no spec (ex.: Material.006 = outline da Fern) -> remove a malha
    if any(colfor(m.name)=='DROP' for m in ob.data.materials if m):
        print('drop spec',ob.name,mnames); bpy.data.objects.remove(ob,do_unlink=True); continue
    for i,slot in enumerate(ob.material_slots):
        mn=slot.material.name if slot.material else ''
        rgb=colfor(mn)
        if rgb is None:
            print('  SEM regra p/',ob.name,mn); continue
        key=str(rgb); mat=cache.get(key) or cache.setdefault(key,mkmat('c'+str(len(cache)),rgb))
        slot.material=mat
    print('paint',ob.name)
# limpa imagens originais pesadas (ja substituidas)
bpy.ops.export_scene.gltf(filepath=dst,export_format='GLB',export_skins=True,export_yup=True)
print('OK',os.path.getsize(dst))
