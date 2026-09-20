package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.model.ai.ModerationDecision;
import java.io.*;import java.nio.file.*;import java.util.*;
/** Offline CSV tool: aiDecision,confidence,manualDecision. It never mutates production data. */
public final class AuditThresholdCalibrator {
 private AuditThresholdCalibrator(){}
 public static void main(String[] args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("Usage: AuditThresholdCalibrator <csv>");var rows=load(Path.of(args[0]));for(double p=.5;p<=.95;p+=.05)for(double r=.5;r<=.95;r+=.05)System.out.println(evaluate(rows,p,r));System.out.println("confidenceBuckets="+buckets(rows));}
 static Metrics evaluate(List<Row> rows,double pass,double reject){long autoPass=0,autoReject=0,falsePass=0,falseReject=0;for(Row x:rows){if(x.ai()==ModerationDecision.PASS&&x.confidence()>=pass){autoPass++;if(x.manual()==ModerationDecision.REJECT)falsePass++;}else if(x.ai()==ModerationDecision.REJECT&&x.confidence()>=reject){autoReject++;if(x.manual()==ModerationDecision.PASS)falseReject++;}}long auto=autoPass+autoReject;return new Metrics(pass,reject,ratio(auto,rows.size()),ratio(rows.size()-auto,rows.size()),ratio(falsePass,autoPass),ratio(falseReject,autoReject));}
 static Map<String,Double>buckets(List<Row> rows){Map<String,Double> out=new LinkedHashMap<>();for(int i=5;i<10;i++){double lo=i/10d,hi=(i+1)/10d;var b=rows.stream().filter(x->x.confidence()>=lo&&x.confidence()<(hi==1?1.000001:hi)).toList();long ok=b.stream().filter(x->x.ai()==x.manual()).count();out.put("%.1f~%.1f".formatted(lo,hi),ratio(ok,b.size()));}return out;}
 static List<Row>load(Path p)throws IOException{return Files.lines(p).skip(1).filter(x->!x.isBlank()).map(x->{String[] c=x.split(",");return new Row(ModerationDecision.valueOf(c[0]),Double.parseDouble(c[1]),ModerationDecision.valueOf(c[2]));}).toList();}
 private static double ratio(long n,long d){return d==0?0:(double)n/d;}
 record Row(ModerationDecision ai,double confidence,ModerationDecision manual){}
 record Metrics(double passThreshold,double rejectThreshold,double automaticCoverage,double manualRate,double falsePassRate,double falseRejectRate){}
}
