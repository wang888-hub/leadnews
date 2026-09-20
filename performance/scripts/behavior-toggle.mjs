const [token,articleId='14',totalArg='100']=process.argv.slice(2)
if(!token)throw new Error('Usage: node behavior-toggle.mjs <token> [articleId] [total]')
const total=Number(totalArg), started=performance.now(), results=[]
await Promise.all(Array.from({length:total},async(_,i)=>{
  const method=i%2===0?'POST':'DELETE', begin=performance.now()
  try{
    const response=await fetch(`http://localhost:51601/api/behavior/articles/${articleId}/like`,{method,headers:{Authorization:`Bearer ${token}`},signal:AbortSignal.timeout(10000)})
    let changed=null;try{changed=(await response.json()).data?.changed??null}catch{}
    results.push({status:response.status,method,changed,ms:performance.now()-begin})
  }catch(error){results.push({status:0,method,changed:null,ms:performance.now()-begin})}
}))
const count=(status,method)=>results.filter(x=>x.status===status&&(!method||x.method===method)).length
console.log(JSON.stringify({scenario:'behavior-toggle',concurrency:total,requests:total,elapsedMs:Math.round(performance.now()-started),success:count(200),http429:count(429),http5xx:results.filter(x=>x.status>=500).length,post200:count(200,'POST'),delete200:count(200,'DELETE'),changed:results.filter(x=>x.changed===true).length},null,2))
