package com.example.industrialhmi
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Typeface
import kotlin.math.*
class MainActivity:Activity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); window.decorView.systemUiVisibility=5894 or 1024 or 512 or 4096; setContentView(Hmi(this))}}
class Hmi(ctx:Context):View(ctx){
 val p=Paint(3); val bg=Color.rgb(23,29,35); val pane=Color.rgb(32,40,47); val edge=Color.rgb(91,106,115); val white=Color.rgb(230,235,237); val mute=Color.rgb(151,164,171); val blue=Color.rgb(0,153,220); val yellow=Color.rgb(244,190,35); val green=Color.rgb(75,181,112); val red=Color.rgb(207,65,55)
 fun set(c:Int,w:Float=1f,s:Paint.Style=Paint.Style.FILL){p.color=c;p.strokeWidth=w;p.style=s;p.strokeCap=Paint.Cap.ROUND}
 fun txt(c:Canvas,t:String,x:Float,y:Float,z:Float,col:Int,b:Boolean=false){set(col);p.textSize=z;p.typeface=if(b) Typeface.DEFAULT_BOLD else Typeface.DEFAULT;c.drawText(t,x,y,p)}
 fun rect(c:Canvas,l:Float,t:Float,r:Float,b:Float,col:Int,stroke:Int?=null){set(col);c.drawRoundRect(RectF(l,t,r,b),5f,5f,p);if(stroke!=null){set(stroke,1.4f,Paint.Style.STROKE);c.drawRoundRect(RectF(l,t,r,b),5f,5f,p)}}
 override fun onDraw(c:Canvas){super.onDraw(c);c.save();c.scale(width/800f,height/480f);c.drawColor(bg)
 rect(c,8f,7f,792f,39f,pane,edge);txt(c,"14:37:25",22f,29f,17f,white,true);txt(c,"SANGINKAR SANAT.CO",310f,28f,17f,white,true);txt(c,"2024-04-30  Tue",610f,28f,14f,mute);rect(c,746f,13f,780f,32f,green);txt(c,"OK",752f,27f,12f,Color.WHITE,true)
 gauge(c,204f,237f,180f,"HORIZONTAL",-180,180,true);gauge(c,424f,237f,180f,"VERTICAL",-30,90,false)
 level(c,650f,"WATER",75,blue);level(c,750f,"FOAM",50,yellow)
 rect(c,10f,458f,790f,474f,pane,edge);txt(c,"SYSTEM READY",22f,470f,10f,green,true);txt(c,"DEMO VALUES",674f,470f,10f,mute,true);c.restore()}
 fun gauge(c:Canvas,x:Float,y:Float,r:Float,title:String,min:Int,max:Int,full:Boolean){
 rect(c,x-r-9,48f,x+r+9,449f,pane,edge);set(edge,1f,Paint.Style.STROKE);c.drawCircle(x,y,r-10,p)
 val start=if(full)135f else 180f;val sweep=if(full)270f else 180f;val aRect=RectF(x-r+8,y-r+8,x+r-8,y+r-8)
 set(green,7f,Paint.Style.STROKE);c.drawArc(aRect,start,sweep*.68f,false,p);set(yellow,7f,Paint.Style.STROKE);c.drawArc(aRect,start+sweep*.68f,sweep*.18f,false,p);set(red,7f,Paint.Style.STROKE);c.drawArc(aRect,start+sweep*.86f,sweep*.14f,false,p)
 val n=if(full)24 else 12
 for(i in 0..n){val a=Math.toRadians((start+sweep*i/n).toDouble());val inn=r-28f;val out=r-12f;set(if(i%2==0)white else mute,if(i%2==0)2f else 1f);c.drawLine(x+cos(a).toFloat()*inn,y+sin(a).toFloat()*inn,x+cos(a).toFloat()*out,y+sin(a).toFloat()*out,p);if(i%2==0){val v=min+(max-min)*i/n;txt(c,"$v",x+cos(a).toFloat()*(r-48f)-8f,y+sin(a).toFloat()*(r-48f)+4f,10f,mute)}}
 val na=Math.toRadians(if(full)270.0 else 180.0);set(red,4f);c.drawLine(x,y,x+cos(na).toFloat()*(r-42f),y+sin(na).toFloat()*(r-42f),p);set(white);c.drawCircle(x,y,8f,p);set(Color.DKGRAY);c.drawCircle(x,y,3f,p)
 rect(c,x-43f,y+48f,x+43f,y+77f,Color.rgb(12,17,21),edge);txt(c,"0.0°",x-27f,y+68f,18f,white,true);rect(c,x-63f,414f,x+63f,438f,Color.rgb(44,55,63),edge);txt(c,title,x-48f,431f,12f,white,true)}
 fun level(c:Canvas,x:Float,title:String,percent:Int,col:Int){txt(c,title,x-24f,30f,12f,white,true);rect(c,x-31f,46f,x+31f,444f,pane,edge);val top=58f;val bot=426f
 for(i in 0..4){val yy=bot-(bot-top)*i/4f;set(mute);c.drawLine(x-39f,yy,x-32f,yy,p);txt(c,"${i*25}",x-58f,yy+4f,9f,mute)}
 val fy=bot-(bot-top)*percent/100f;rect(c,x-19f,top,x+19f,bot,Color.rgb(12,17,21));rect(c,x-19f,fy,x+19f,bot,col);set(white,1.2f,Paint.Style.STROKE);c.drawRoundRect(RectF(x-19f,top,x+19f,bot),3f,3f,p);rect(c,x-29f,390f,x+29f,416f,Color.rgb(12,17,21),edge);txt(c,"$percent%",x-22f,408f,13f,white,true)}
}
