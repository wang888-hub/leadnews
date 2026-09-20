const login=await fetch('http://localhost:51601/api/wemedia/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:'wemedia_demo',password:'password'})})
const token=(await login.json()).data.token,controller=new AbortController(),started=performance.now()
let cancelled=false,firstChunkMs=null
try{
  const response=await fetch('http://localhost:51601/api/wemedia/news/22/ai/continue',{method:'POST',signal:controller.signal,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json',Accept:'text/event-stream'},body:JSON.stringify({instruction:'stage15 取消与 permit 释放验证',targetLength:300,currentContent:'可靠系统需要在客户端断开时及时释放资源。'})})
  if(!response.ok)throw new Error(`HTTP ${response.status}`)
  const reader=response.body.getReader(),decoder=new TextDecoder();let buffer=''
  while(firstChunkMs===null){const x=await reader.read();if(x.done)break;buffer+=decoder.decode(x.value,{stream:true});if(/event:chunk/.test(buffer)){firstChunkMs=Math.round(performance.now()-started);controller.abort();await reader.read()}}
}catch(error){cancelled=error.name==='AbortError'}
console.log(JSON.stringify({cancelled,firstChunkMs,totalMs:Math.round(performance.now()-started)}))
