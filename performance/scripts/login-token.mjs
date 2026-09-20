const [kind='user']=process.argv.slice(2)
const accounts={user:['/api/user/login',{phone:'13800000000',password:'password'}],wemedia:['/api/wemedia/login',{name:'wemedia_demo',password:'password'}],admin:['/api/admin/login',{name:'admin_demo',password:'password'}]}
if(!accounts[kind])throw new Error('kind must be user, wemedia, or admin')
const [path,body]=accounts[kind]
const response=await fetch(`http://localhost:51601${path}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)})
if(!response.ok)throw new Error(`login failed: ${response.status}`)
process.stdout.write((await response.json()).data.token)
