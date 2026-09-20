package com.aaliyun.leadnews.ai.service;

import com.aaliyun.leadnews.ai.config.*;
import com.aaliyun.leadnews.ai.exception.AiExceptions;
import com.aaliyun.leadnews.ai.model.AiImageInput;
import io.minio.*;
import org.springframework.stereotype.Service;

import java.util.*;import java.io.*;import java.awt.Color;import java.awt.Graphics2D;import java.awt.RenderingHints;import java.awt.image.BufferedImage;import javax.imageio.*;import javax.imageio.stream.ImageOutputStream;

@Service
public class TrustedMediaLoader {

 private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");

 private final MinioClient minio;
 private final ArticleAuditProperties audit;
 private final AiProperties ai;

 public TrustedMediaLoader(MinioClient m, ArticleAuditProperties a, AiProperties p) {
  minio = m;
  audit = a;
  ai = p;
 }

 public List<AiImageInput> load(List<String> keys) {
  List<String> distinct = keys == null ? List.of() : keys.stream().distinct().toList();
  if (distinct.size() > audit.maxImages()) {
   throw new AiExceptions.InvalidImage("Article has too many images for automatic audit");
  }
  List<AiImageInput> result = new ArrayList<>();
  long total=0;
  for (String key : distinct) {
   AiImageInput image=loadOne(key);total+=image.bytes().length;
   if(total>audit.maxTotalImageBytes()||base64Size(total)>audit.maxRequestBytes())throw new AiExceptions.InvalidImage("Audit image request exceeds configured total size");
   result.add(image);
  }
  return result;
 }

 private AiImageInput loadOne(String key) {
  if (key == null
          || key.isBlank()
          || key.contains("..")
          || key.startsWith("/")
          || audit.trustedPrefixes().stream().noneMatch(key::startsWith)) {
   throw new AiExceptions.InvalidImage("Untrusted media object key");
  }
  try {
   var stat = minio.statObject(
           StatObjectArgs.builder().bucket(audit.bucket()).object(key).build());
   if (stat.size() > audit.maxSourceImageBytes()) {
    throw new AiExceptions.InvalidImage("Image exceeds configured size limit");
   }
   String type = stat.contentType();
   if (!TYPES.contains(type)) {
    throw new AiExceptions.InvalidImage("Unsupported image MIME type");
   }
   try (var stream = minio.getObject(
           GetObjectArgs.builder().bucket(audit.bucket()).object(key).build())) {
    byte[] source=readBounded(stream,audit.maxSourceImageBytes());
    return new AiImageInput("image/jpeg",compress(source),null);
   }
  } catch (AiExceptions.InvalidImage e) {
   throw e;
  } catch (Exception e) {
   throw new AiExceptions.ProviderUnavailable();
  }
 }

 private byte[] readBounded(InputStream in,long max)throws IOException{byte[] data=in.readNBytes(Math.toIntExact(max+1));if(data.length>max)throw new AiExceptions.InvalidImage("Image exceeds configured source size limit");return data;}
 byte[] compress(byte[] source)throws IOException{
  BufferedImage original=ImageIO.read(new ByteArrayInputStream(source));if(original==null)throw new AiExceptions.InvalidImage("Image payload cannot be decoded");
  int w=original.getWidth(),h=original.getHeight();double scale=Math.min(1d,(double)audit.maxEdge()/Math.max(w,h));
  int tw=Math.max(1,(int)Math.round(w*scale)),th=Math.max(1,(int)Math.round(h*scale));
  BufferedImage rgb=new BufferedImage(tw,th,BufferedImage.TYPE_INT_RGB);Graphics2D g=rgb.createGraphics();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.setColor(Color.WHITE);g.fillRect(0,0,tw,th);g.drawImage(original,0,0,tw,th,null);}finally{g.dispose();original.flush();}
  float quality=(float)audit.jpegQuality();byte[] out;
  do{out=jpeg(rgb,quality);quality-=.1f;}while(out.length>audit.maxAuditImageBytes()&&quality>=.3f);
  rgb.flush();if(out.length>audit.maxAuditImageBytes())throw new AiExceptions.InvalidImage("Compressed audit image still exceeds configured limit");return out;
 }
 private byte[] jpeg(BufferedImage image,float quality)throws IOException{var writers=ImageIO.getImageWritersByFormatName("jpeg");if(!writers.hasNext())throw new IOException("JPEG writer unavailable");var writer=writers.next();try(var out=new ByteArrayOutputStream();ImageOutputStream ios=ImageIO.createImageOutputStream(out)){writer.setOutput(ios);var param=writer.getDefaultWriteParam();param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);param.setCompressionQuality(quality);writer.write(null,new javax.imageio.IIOImage(image,null,null),param);return out.toByteArray();}finally{writer.dispose();}}
 private long base64Size(long bytes){return 4*((bytes+2)/3);}
}
