package de.robinthor.digiworldexplorer.detection

/**
 * Findet den blauen Dash-Knopf unterhalb des Spielfelds. Absolute Pixelkoordinaten waeren
 * geraete- und panelabhaengig, deshalb wird der Knopf ueber seine Farbe gesucht: er ist die
 * grosse, kraeftig cyan-blaue Flaeche unterhalb des Rasters; bei mehreren blauen Tasten gewinnt die linke. Der dunkelblaue
 * Ueberschriftentext faellt durch die Gruen-Untergrenze heraus.
 */
object DashButtonLocator{
 private const val MIN_SAMPLES=350
 private const val STEP=2
 /** Piksel yang cocok dikelompokkan ke sel kasar sebelum dicari bagian yang saling terhubung. */
 private const val CELL=12
 private const val CELL_MIN_HITS=6
 /** UI baru punya DUA tombol bulat biru yang mirip. Tombol Dash = tombol paling KIRI.
  *  Kalau yang terketuk ternyata tombol sebelahnya, ubah ke false (ambil yang paling kanan). */
 private const val DASH_IS_LEFTMOST=true

 private class Blob(val x:Double,val y:Double,val n:Int)

 fun locate(width:Int,height:Int,argb:IntArray,bounds:GridBounds):Pair<Float,Float>?{
  val y0=(bounds.bottom+ (bounds.bottom-bounds.top)/20).coerceIn(0,height-1)
  if(y0>=height-10)return null
  val cols=(width+CELL-1)/CELL;val rows=(height+CELL-1)/CELL
  val hits=IntArray(cols*rows);val sx=LongArray(cols*rows);val sy=LongArray(cols*rows)
  var y=y0
  while(y<height){
   var x=0
   while(x<width){
    val p=argb[y*width+x];val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255
    if(b>195&&g>140&&r<140&&b>r+80){val i=(y/CELL)*cols+x/CELL;hits[i]++;sx[i]+=x;sy[i]+=y}
    x+=STEP
   }
   y+=STEP
  }
  // Bagian-bagian biru yang saling terhubung (8 tetangga) = satu tombol.
  val seen=BooleanArray(hits.size);val blobs=mutableListOf<Blob>()
  fun blobOf(cells:List<Int>):Blob?{
   var n=0;var X=0L;var Y=0L
   for(c in cells){n+=hits[c];X+=sx[c];Y+=sy[c]}
   return if(n>=MIN_SAMPLES)Blob(X.toDouble()/n,Y.toDouble()/n,n) else null
  }
  for(start in hits.indices){
   if(seen[start]||hits[start]<CELL_MIN_HITS)continue
   val cells=mutableListOf<Int>();val stack=ArrayDeque<Int>();stack.add(start);seen[start]=true
   while(stack.isNotEmpty()){
    val c=stack.removeLast();cells+=c;val cr=c/cols;val cc=c%cols
    for(dr in -1..1)for(dc in -1..1){
     val nr=cr+dr;val nc=cc+dc
     if(nr<0||nc<0||nr>=rows||nc>=cols)continue
     val ni=nr*cols+nc
     if(!seen[ni]&&hits[ni]>=CELL_MIN_HITS){seen[ni]=true;stack.add(ni)}
    }
   }
   val minC=cells.minOf{it%cols};val maxC=cells.maxOf{it%cols};val minR=cells.minOf{it/cols};val maxR=cells.maxOf{it/cols}
   val w=maxC-minC+1;val h=maxR-minR+1
   if(w>h*1.7){
    // Dua tombol yang saling menempel terbaca sebagai satu gumpalan lebar: dibelah di tengah.
    val (left,right)=cells.partition{(it%cols)*2<=minC+maxC}
    blobOf(left)?.let{blobs+=it};blobOf(right)?.let{blobs+=it}
   } else blobOf(cells)?.let{blobs+=it}
  }
  val pick=(if(DASH_IS_LEFTMOST)blobs.minByOrNull{it.x} else blobs.maxByOrNull{it.x})?:return null
  return pick.x.toFloat() to pick.y.toFloat()
 }

 /** Sicherer relativer Mittelpunkt nur fuer den expliziten experimentellen Dash-Spam-Fallback. */
 fun relativeFallback(width:Int,height:Int,bounds:GridBounds):Pair<Float,Float>?{
  val gridWidth=(bounds.right-bounds.left).toFloat();val gridHeight=(bounds.bottom-bounds.top).toFloat()
  if(gridWidth<=0f||gridHeight<=0f)return null
  val x=bounds.left+gridWidth*.66f;val y=bounds.bottom+gridHeight*.60f
  if(x !in 0f..width.toFloat()||y !in 0f..height.toFloat())return null
  return x to y
 }
}
