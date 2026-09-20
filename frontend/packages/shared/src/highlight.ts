export interface HighlightPart { text:string; highlighted:boolean }
const decode=(value:string)=>{const el=document.createElement('textarea');el.innerHTML=value;return el.value}
export function parseHighlight(value:string):HighlightPart[]{const parts:HighlightPart[]=[];value.split(/(<\/?em>)/i).forEach(token=>{if(!token)return;if(/^<em>$/i.test(token)){parts.push({text:'',highlighted:true});return}if(/^<\/em>$/i.test(token))return;const last=parts.at(-1);if(last?.highlighted&&!last.text)last.text=decode(token);else parts.push({text:decode(token),highlighted:false})});return parts}
