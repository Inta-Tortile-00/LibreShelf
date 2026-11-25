package org.libreshelf
import android.app.*
import android.appwidget.*
import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews

object Art {
 fun book(lib:Library,b:Book,w:Int,h:Int):Bitmap { val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);val c=Canvas(out);val p=Paint(3);val r=RectF(w*.10f,h*.035f,w*.90f,h*.99f);val radius=lib.p.getInt("radius",2).toFloat()*w/100; val shadow=lib.p.getInt("shadow",65);val light=lib.p.getInt("light",75);val spine=lib.p.getInt("spine",55);p.color=Color.argb(shadow*2,0,0,0);p.maskFilter=BlurMaskFilter((w*(.005f+lib.p.getInt("softness",50)/1000f)).coerceAtLeast(1f),BlurMaskFilter.Blur.NORMAL);val s=RectF(r);s.offset(w*.025f,0f);c.drawRoundRect(s,radius,radius,p);p.maskFilter=null;p.color=Color.rgb(60+Math.floorMod(b.title.hashCode(),75),75,69);c.drawRoundRect(r,radius,radius,p);val path=Path().apply{addRoundRect(r,radius,radius,Path.Direction.CW)};c.save();c.clipPath(path);val cover=BitmapFactory.decodeFile(lib.cover(b).path);if(cover!=null){c.drawBitmap(cover,null,r,p);cover.recycle()}else{p.color=Color.rgb(239,222,184);p.style=Paint.Style.STROKE;p.strokeWidth=1f;c.drawRect(r.left+6,r.top+7,r.right-6,r.bottom-7,p);p.style=Paint.Style.FILL;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.create("serif",Typeface.BOLD);p.textSize=w*.105f;val lines=mutableListOf<String>();var line="";for(word in b.title.split(' ')){if(p.measureText("$line $word")>r.width()-12&&line.isNotBlank()){lines.add(line);line=word}else line=("$line $word").trim()};if(line.isNotBlank())lines.add(line);lines.take(5).forEachIndexed{i,t->c.drawText(t,r.centerX(),h*.26f+i*w*.13f,p)};p.textSize=w*.075f;c.drawText(b.format.uppercase(),r.centerX(),h*.88f,p)};p.shader=LinearGradient(r.left,0f,r.right,0f,intArrayOf(Color.argb(spine*2,0,0,0),Color.argb(light,255,255,255),Color.argb(spine,0,0,0),Color.TRANSPARENT,Color.TRANSPARENT,Color.argb(35,0,0,0)),floatArrayOf(0f,.035f,.065f,.13f,.95f,1f),Shader.TileMode.CLAMP);c.drawRect(r,p);p.shader=null;c.restore();return out }
 fun plank(lib:Library):Bitmap {
  val b=Bitmap.createBitmap(480,24,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(3)
  val theme=lib.p.getString("theme","Oak");if(theme=="Invisible")return b
  p.color=Color.argb(lib.p.getInt("shadow",65)*2,0,0,0);p.maskFilter=BlurMaskFilter(1f+lib.p.getInt("softness",50)*.06f,BlurMaskFilter.Blur.NORMAL);c.drawRect(5f,9f,475f,18f,p);p.maskFilter=null
  val colors=when(theme){"Walnut"->intArrayOf(0xffb78b60.toInt(),0xff725034.toInt(),0xff352218.toInt());"Mahogany"->intArrayOf(0xffc18462.toInt(),0xff8b4935.toInt(),0xff432019.toInt());"Birch"->intArrayOf(0xffffe9bc.toInt(),0xffd7b982.toInt(),0xff987449.toInt());"Graphite"->intArrayOf(0xffaaaaaa.toInt(),0xff666666.toInt(),0xff303030.toInt());"Glass"->intArrayOf(0xaaddffff.toInt(),0x888eb8c0.toInt(),0x66507880);else->intArrayOf(0xfff0d49d.toInt(),0xffc8a46d.toInt(),0xff755130.toInt())}
  val rect=RectF(2f,1f,478f,15f);p.shader=LinearGradient(0f,1f,0f,15f,colors,floatArrayOf(0f,.35f,1f),Shader.TileMode.CLAMP);c.drawRoundRect(rect,2f,2f,p);p.shader=null
  if(theme in listOf("Oak","Walnut","Mahogany","Birch")){
   c.save();c.clipPath(Path().apply{addRoundRect(rect,2f,2f,Path.Direction.CW)});val rng=java.util.Random(73);p.style=Paint.Style.STROKE
   val grain=lib.p.getInt("grain",65);for(i in 0 until 65){val y=1f+rng.nextFloat()*14f;p.color=Color.argb(grain*(15+rng.nextInt(45))/100,if(i%4==0)255 else 45,if(i%4==0)235 else 22,if(i%4==0)190 else 10);p.strokeWidth=.25f+rng.nextFloat()*.55f;val path=Path();path.moveTo(0f,y);for(x in 12..480 step 12)path.lineTo(x.toFloat(),y+kotlin.math.sin(x*.026f+i)*.65f+rng.nextFloat()*.3f);c.drawPath(path,p)};p.style=Paint.Style.FILL;c.restore()
  }
  p.color=Color.argb(lib.p.getInt("light",75)*2,255,255,255);c.drawRect(4f,1f,476f,1.8f,p);return b
 }
}

class ShelfWidget:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ids.forEach{render(c,m,it)};schedule(c)}
 override fun onAppWidgetOptionsChanged(c:Context,m:AppWidgetManager,id:Int,o:Bundle){render(c,m,id)}
 override fun onDeleted(c:Context,ids:IntArray){val e=Library(c).p.edit();ids.forEach{e.remove("page:$it")};e.apply();schedule(c)}
 override fun onDisabled(c:Context){alarm(c).cancel(tick(c))}
 fun handleRotation(c:Context,i:Intent){val m=AppWidgetManager.getInstance(c);val ids=m.getAppWidgetIds(ComponentName(c,ShelfWidget::class.java));when(i.action){
  NEXT,PREV->{val id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(id in ids)render(c,m,id,if(i.action==NEXT)1 else -1)}
  TICK->{if(Library(c).p.getString("rotation","Off")!="Off")ids.forEach{render(c,m,it,1)};schedule(c)}
 }}
 companion object {
 const val NEXT="org.libreshelf.NEXT";const val PREV="org.libreshelf.PREV";const val TICK="org.libreshelf.TICK"
 private fun alarm(c:Context)=c.getSystemService(AlarmManager::class.java)
 private fun tick(c:Context)=PendingIntent.getBroadcast(c,0,Intent(c,RotationReceiver::class.java).setAction(TICK),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
 fun schedule(c:Context){val a=alarm(c);val pi=tick(c);a.cancel(pi);val minutes=Library(c).p.getString("rotation","Off")?.toLongOrNull()?:return;val m=AppWidgetManager.getInstance(c);if(m.getAppWidgetIds(ComponentName(c,ShelfWidget::class.java)).isEmpty())return;a.set(AlarmManager.ELAPSED_REALTIME,android.os.SystemClock.elapsedRealtime()+minutes.coerceAtLeast(15)*60000L,pi)}
 fun refresh(c:Context){val m=AppWidgetManager.getInstance(c);m.getAppWidgetIds(ComponentName(c,ShelfWidget::class.java)).forEach{render(c,m,it)};schedule(c)}
 fun render(c:Context,m:AppWidgetManager,id:Int,delta:Int=0){
 val lib=Library(c);val opt=m.getAppWidgetOptions(id);val width=opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300).coerceAtLeast(100);val height=opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,110).coerceAtLeast(75)
 val cols=lib.p.getInt("count",5).coerceIn(2,8).coerceAtMost((width/40).coerceAtLeast(2));val wd=((width-12)/cols).coerceIn(30,80);val all=lib.visible();val nav=all.size>cols;val available=(height-if(nav)32 else 0).coerceAtLeast(43);val hd=(wd*1.25).toInt().coerceAtMost((available-12).coerceAtLeast(25));val rows=(available/(hd+10)).coerceIn(1,3);val capacity=rows*cols
 val pins=if(lib.p.getString("pinRotation","Rotate all")=="Keep pins visible")all.filter{it.pinned}.take((capacity-1).coerceAtLeast(0)) else emptyList();val pool=all.filterNot{it in pins};val slots=capacity-pins.size;val pages=((pool.size+slots-1)/slots).coerceAtLeast(1);val page=Math.floorMod(lib.p.getInt("page:$id",0)+delta,pages);lib.p.edit().putInt("page:$id",page).apply();val books=pins+pool.drop(page*slots).take(slots)
 val rv=RemoteViews(c.packageName,R.layout.shelf);rv.removeAllViews(R.id.rows);val settings=PendingIntent.getActivity(c,id,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
 rv.setViewVisibility(R.id.navigation,if(pages>1)android.view.View.VISIBLE else android.view.View.GONE);rv.setTextViewText(R.id.page,"${page+1} / $pages")
 fun navIntent(action:String)=PendingIntent.getBroadcast(c,id,Intent(c,RotationReceiver::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
 rv.setOnClickPendingIntent(R.id.previous,navIntent(PREV));rv.setOnClickPendingIntent(R.id.next,navIntent(NEXT));rv.setOnClickPendingIntent(R.id.page,settings)
 for(row in 0 until rows){val r=RemoteViews(c.packageName,R.layout.row);r.setImageViewBitmap(R.id.plank,Art.plank(lib));r.setOnClickPendingIntent(R.id.plank,settings);for(col in 0 until cols){val b=books.getOrNull(row*cols+col)?:continue;val item=RemoteViews(c.packageName,R.layout.book);val bmp=Art.book(lib,b,wd,hd);bmp.density=160;item.setImageViewBitmap(R.id.cover,bmp);item.setContentDescription(R.id.cover,b.title);val i=Intent(c,OpenBook::class.java).setData(Uri.parse(b.uri));item.setOnClickPendingIntent(R.id.cover,PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT));r.addView(R.id.books,item)};rv.addView(R.id.rows,r);if(books.size<=(row+1)*cols)break};m.updateAppWidget(id,rv)
 }
 }
}
class RotationReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action==Intent.ACTION_BOOT_COMPLETED)ShelfWidget.refresh(c) else ShelfWidget().handleRotation(c,i)}}
class OpenBook:Activity(){override fun onCreate(s:Bundle?){super.onCreate(s);val lib=Library(this);val b=lib.books().find{it.uri==intent.dataString};if(b==null){finish();return};val i=Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(b.uri),b.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);val pkg=lib.p.getString("reader:${b.uri}",null)?:lib.p.getString("reader:${b.format}",null)?:lib.p.getString("reader",null);if(!pkg.isNullOrBlank())i.setPackage(pkg);try{startActivity(if(pkg.isNullOrBlank())Intent.createChooser(i,"Open ${b.title}") else i);finish()}catch(e:ActivityNotFoundException){i.setPackage(null);try{startActivity(Intent.createChooser(i,"Choose another reader"));finish()}catch(e:Exception){AlertDialog.Builder(this).setMessage("No compatible reader found.").setPositiveButton("OK"){_,_->finish()}.setOnCancelListener{finish()}.show()}}catch(e:SecurityException){AlertDialog.Builder(this).setMessage("Book access was revoked. Select it again in LibreShelf.").setPositiveButton("OK"){_,_->finish()}.setOnCancelListener{finish()}.show()}}}
