const [concurrencyArg='1',newsId='22']=process.argv.slice(2)
const concurrency=Number(concurrencyArg)
const login=await fetch('http://localhost:51601/api/wemedia/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:'wemedia_demo',password:'password'})})
const token=(await login.json()).data.token
async function one(i){
  const started=performance.now()
  const response=await fetch(`http://localhost:51601/api/wemedia/news/${newsId}/ai/continue`,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json',Accept:'text/event-stream'},body:JSON.stringify({instruction:`stage15 温和并发续写 ${i}`,targetLength:120,currentContent:'可靠系统需要清晰的边界、可观测性和恢复机制。'})})
  if(response.status!==200){await response.arrayBuffer();return{http:response.status,chunks:0,done:0,firstTokenMs:null,totalMs:Math.round(performance.now()-started)}}
  const reader=response.body.getReader(),decoder=new TextDecoder();let buffer='',chunks=0,done=0,firstTokenMs=null
  while(true){const x=await reader.read();buffer+=decoder.decode(x.value||new Uint8Array(),{stream:!x.done});const blocks=buffer.split(/\r?\n\r?\n/);buffer=blocks.pop()||'';for(const b of blocks){if(/^event:chunk/m.test(b)){chunks++;firstTokenMs??=Math.round(performance.now()-started)}if(/^event:done/m.test(b))done++}if(x.done)break}
  return{http:200,chunks,done,firstTokenMs,totalMs:Math.round(performance.now()-started)}
}
const results=await Promise.all(Array.from({length:concurrency},(_,i)=>one(i)))
console.log(JSON.stringify({scenario:'sse',concurrency,success:results.filter(x=>x.http===200).length,http429:results.filter(x=>x.http===429).length,results},null,2))
