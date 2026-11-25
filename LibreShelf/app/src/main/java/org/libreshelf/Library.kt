package org.libreshelf

import android.content.*
import android.database.Cursor
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract as DC
import android.provider.OpenableColumns
import org.json.*
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipFile

 data class Book(val uri:String,val title:String,val author:String,val format:String,val pinned:Boolean=false,val folder:String="") {
 fun json()=JSONObject().put("uri",uri).put("title",title).put("author",author).put("format",format).put("pinned",pinned).put("folder",folder)
 val mime get()=when(format){"pdf"->"application/pdf";"epub"->"application/epub+zip";else->"application/vnd.comicbook+zip"}
 }
class Library(val c:Context, preferences:String="library") {
 val p=c.getSharedPreferences(preferences,0)
 fun books():List<Book> { val a=JSONArray(p.getString("books","[]")); return (0 until a.length()).map { val j=a.getJSONObject(it); Book(j.getString("uri"),j.getString("title"),j.optString("author"),j.getString("format"),j.optBoolean("pinned"),j.optString("folder")) } }
 fun save(b:List<Book>) { p.edit().putString("books",JSONArray(b.map{it.json()}).toString()).commit() }
 fun folders()=p.getStringSet("folders",emptySet())!!.toSet()
 fun addFolder(u:String){p.edit().putStringSet("folders",folders()+u).commit()}
 fun cover(b:Book)=File(c.filesDir,"covers/${MessageDigest.getInstance("SHA-256").digest(b.uri.toByteArray()).joinToString(""){"%02x".format(it)}}.png")
 fun visible():List<Book>{ val b=books(); val mode=p.getString("mode","Hybrid"); return b.filter{when(mode){"Permanent"->it.pinned;"Dynamic"->it.folder.isNotEmpty();else->true}}.sortedWith(compareByDescending<Book>{it.pinned}.thenBy{it.title.lowercase()}) }
 fun name(u:Uri):String=c.contentResolver.query(u,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0) else null}?:u.lastPathSegment.orEmpty()
 fun scan(tree:Uri):List<Book>{ val out=mutableListOf<Book>(); val seen=hashSetOf<String>(); val todo=java.util.ArrayDeque<String>(); todo.add(DC.getTreeDocumentId(tree)); while(todo.isNotEmpty()){ val id=todo.removeFirst(); if(!seen.add(id))continue; val child=DC.buildChildDocumentsUriUsingTree(tree,id); val cols=arrayOf(DC.Document.COLUMN_DOCUMENT_ID,DC.Document.COLUMN_DISPLAY_NAME,DC.Document.COLUMN_MIME_TYPE); c.contentResolver.query(child,cols,null,null,null)?.use{ q->while(q.moveToNext()){val d=q.getString(0); val n=q.getString(1); if(q.getString(2)==DC.Document.MIME_TYPE_DIR)todo.add(d) else { val u=DC.buildDocumentUriUsingTree(tree,d); if(n.substringAfterLast('.').lowercase() in listOf("epub","pdf","cbz"))out.add(parse(u,n,false,tree.toString())) }}} ?: throw IOException("Folder unavailable") }; return out }
 private fun xml(input:InputStream, visit:(XmlPullParser)->Unit){input.use{ val x=Xml.newPullParser(); x.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true); x.setInput(it,null); while(x.eventType!=XmlPullParser.END_DOCUMENT){if(x.eventType==XmlPullParser.START_TAG)visit(x); x.next()} }}
 private fun ZipFile.readSmall(path:String, max:Int=16*1024*1024):ByteArray? { val e=getEntry(path)?:return null; if(e.size>max)return null; return getInputStream(e).use{ val o=ByteArrayOutputStream(); val buf=ByteArray(8192); var total=0; while(true){val n=it.read(buf); if(n<0)break; total+=n;if(total>max)return null;o.write(buf,0,n)};o.toByteArray()} }
 private fun decode(data:ByteArray):Bitmap? { val o=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(data,0,data.size,o); if(o.outWidth<=0||o.outHeight<=0)return null; var s=1;while(maxOf(o.outWidth,o.outHeight)/s>700)s*=2;return BitmapFactory.decodeByteArray(data,0,data.size,BitmapFactory.Options().apply{inSampleSize=s}) }
 fun parse(u:Uri,n:String,pin:Boolean,folder:String=""):Book { val format=n.substringAfterLast('.').lowercase(); require(format in listOf("epub","pdf","cbz")); var title=n.substringBeforeLast('.');var author="";var bitmap:Bitmap?=null; val temp=File.createTempFile("book-", ".tmp",c.cacheDir)
 try { if(format=="pdf"){c.contentResolver.openFileDescriptor(u,"r")?.let{ fd-> try{PdfRenderer(fd).use{r->if(r.pageCount>0)r.openPage(0).use{page-> val w=400;val h=(w.toFloat()*page.height/page.width).toInt().coerceIn(1,700);bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.WHITE)};page.render(bitmap!!,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)}}}catch(e:Exception){runCatching{fd.close()};throw e}} }
 else { c.contentResolver.openInputStream(u)?.use{ input->temp.outputStream().use{input.copyTo(it)}}?:throw IOException("Cannot open book"); ZipFile(temp).use{ z-> if(format=="cbz"){z.readSmall("ComicInfo.xml",1024*1024)?.let{xml(it.inputStream()){x->when(x.name){"Title"->title=x.nextText().ifBlank{title};"Writer"->author=x.nextText()}}};val first=z.entries().asSequence().filter{!it.isDirectory&&it.name.substringAfterLast('.').lowercase() in listOf("jpg","jpeg","png","webp")&&!it.name.startsWith("__MACOSX/")}.map{it.name}.sortedWith(compareBy<String>{it.replace(Regex("\\d+")){m->m.value.padStart(12,'0')}}).firstOrNull();bitmap=first?.let{z.readSmall(it)}?.let{decode(it)} }
 else { var opf="";z.readSmall("META-INF/container.xml",1024*1024)?.let{xml(it.inputStream()){x->if(x.name=="rootfile"&&opf.isEmpty())opf=x.getAttributeValue(null,"full-path")?:""}};val items=mutableMapOf<String,String>();var coverId="";var coverHref="";z.readSmall(opf,4*1024*1024)?.let{xml(it.inputStream()){x->when(x.name){"title"->title=x.nextText().ifBlank{title};"creator"->if(author.isEmpty())author=x.nextText();"meta"->if(x.getAttributeValue(null,"name")=="cover")coverId=x.getAttributeValue(null,"content")?:"";"item"->{val href=x.getAttributeValue(null,"href")?:"";items[x.getAttributeValue(null,"id")?:""]=href;if(x.getAttributeValue(null,"properties").orEmpty().split(' ').contains("cover-image"))coverHref=href}}}};val href=coverHref.ifBlank{items[coverId].orEmpty()}; if(href.isNotBlank()){val path=java.net.URI("/"+opf).resolve(href).normalize().path.removePrefix("/");bitmap=z.readSmall(path)?.let{decode(it)}} }
 }} }catch(e:Exception){android.util.Log.w("LibreShelf","Cover extraction failed",e)}finally{temp.delete()}
 val b=Book(u.toString(),title,author,format,pin,folder);bitmap?.let{val f=cover(b);f.parentFile?.mkdirs();f.outputStream().use{out->it.compress(Bitmap.CompressFormat.PNG,100,out)};it.recycle()};return b }
}
