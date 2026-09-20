import {mkdir, writeFile} from 'node:fs/promises'
import {dirname} from 'node:path'
import {createHmac} from 'node:crypto'

const args = Object.fromEntries(process.argv.slice(2).map(x => {
  const i=x.indexOf('='); return i<0 ? [x.replace(/^--/,''),true] : [x.slice(2,i),x.slice(i+1)]
}))
if (!args.url) throw new Error('Required: --url=http://localhost:51601/api/...')
const concurrency=Number(args.concurrency||1), warmup=Number(args.warmup||30), duration=Number(args.duration||60)
const requests=args.requests ? Number(args.requests) : null
const method=String(args.method||'GET').toUpperCase()
const thinkMs=Number(args.think||0)
const headers={'Accept':'application/json'}
if(args.token) headers.Authorization=`Bearer ${args.token}`
if(args.body){headers['Content-Type']='application/json'}
const body=args.body ? args.body : undefined

const percentile=(sorted,p)=>sorted.length?sorted[Math.min(sorted.length-1,Math.ceil(sorted.length*p)-1)]:0
const jwtUsers=Number(args['jwt-users']||0), jwtStart=Number(args['jwt-start']||1500000)
function tokenFor(index){
  if(!jwtUsers)return args.token
  const secret=process.env.JWT_SECRET
  if(!secret||Buffer.byteLength(secret)<32)throw new Error('JWT_SECRET >=32 bytes is required for synthetic stage15 users')
  const now=Math.floor(Date.now()/1000), userId=jwtStart+(index%jwtUsers)
  const enc=x=>Buffer.from(JSON.stringify(x)).toString('base64url')
  const input=`${enc({alg:'HS256'})}.${enc({sub:String(userId),userId,clientType:'APP_USER',iat:now,exp:now+3600})}`
  return `${input}.${createHmac('sha256',secret).update(input).digest('base64url')}`
}
async function requestOnce(record,index=0) {
  const started=performance.now()
  try {
    const requestHeaders={...headers};const token=tokenFor(index);if(token)requestHeaders.Authorization=`Bearer ${token}`
    const response=await fetch(args.url,{method,headers:requestHeaders,body,signal:AbortSignal.timeout(Number(args.timeout||10000))})
    await response.arrayBuffer()
    if(record) record.push({status:response.status,ms:performance.now()-started})
  } catch(error) { if(record) record.push({status:0,ms:performance.now()-started,error:error.name}) }
  if(thinkMs>0) await new Promise(resolve=>setTimeout(resolve,thinkMs))
}
async function durationRun(seconds,record) {
  const until=performance.now()+seconds*1000
  let index=0
  await Promise.all(Array.from({length:concurrency},async()=>{while(performance.now()<until)await requestOnce(record,index++)}))
}
async function countRun(total,record) {
  let next=0
  await Promise.all(Array.from({length:Math.min(concurrency,total)},async()=>{while(true){const i=next++;if(i>=total)return;await requestOnce(record,i)}}))
}

if(warmup>0) await durationRun(warmup,null)
const records=[], started=new Date(), clock=performance.now()
if(requests!==null) await countRun(requests,records); else await durationRun(duration,records)
const elapsed=(performance.now()-clock)/1000, latencies=records.map(x=>x.ms).sort((a,b)=>a-b)
const successfulLatencies=records.filter(x=>x.status>=200&&x.status<300).map(x=>x.ms).sort((a,b)=>a-b)
const count=s=>records.filter(x=>x.status===s).length
const result={
  scenario:args.scenario||'http',url:args.url,method,concurrency,warmupSeconds:warmup,
  configuredDurationSeconds:requests===null?duration:null,configuredRequests:requests,
  startedAt:started.toISOString(),elapsedSeconds:Number(elapsed.toFixed(3)),requests:records.length,
  success:records.filter(x=>x.status>=200&&x.status<300).length,http429:count(429),http5xx:records.filter(x=>x.status>=500).length,
  connectionErrors:count(0),rps:Number((records.length/elapsed).toFixed(2)),
  latencyMs:{p50:Number(percentile(latencies,.5).toFixed(2)),p95:Number(percentile(latencies,.95).toFixed(2)),p99:Number(percentile(latencies,.99).toFixed(2)),max:Number((latencies.at(-1)||0).toFixed(2))},
  successfulLatencyMs:{p50:Number(percentile(successfulLatencies,.5).toFixed(2)),p95:Number(percentile(successfulLatencies,.95).toFixed(2)),p99:Number(percentile(successfulLatencies,.99).toFixed(2)),max:Number((successfulLatencies.at(-1)||0).toFixed(2))},
  statuses:Object.fromEntries([...new Set(records.map(x=>x.status))].sort((a,b)=>a-b).map(s=>[s,count(s)]))
}
const json=JSON.stringify(result,null,2)
if(args.out){await mkdir(dirname(args.out),{recursive:true});await writeFile(args.out,json+'\n')}
console.log(json)
