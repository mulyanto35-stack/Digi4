package de.robinthor.digiworldexplorer.detection
import org.junit.Assert.*
import org.junit.Test
class DashButtonLocatorTest{
 private val w=720;private val h=1600
 private val bounds=GridBounds(18,300,702,984)
 private fun frame(vararg circles:Triple<Int,Int,Int>):IntArray{
  val a=IntArray(w*h){0xFF101020.toInt()}
  for((cx,cy,r) in circles)for(y in cy-r..cy+r)for(x in cx-r..cx+r)if((x-cx)*(x-cx)+(y-cy)*(y-cy)<=r*r)a[y*w+x]=(0xFF shl 24) or (40 shl 16) or (200 shl 8) or 255
  return a
 }
 @Test fun singleButtonIsCentered(){
  val p=DashButtonLocator.locate(w,h,frame(Triple(500,1200,60)),bounds)!!
  assertEquals(500f,p.first,3f);assertEquals(1200f,p.second,3f)
 }
 @Test fun twoSeparateButtonsPickLeft(){
  val p=DashButtonLocator.locate(w,h,frame(Triple(300,1250,60),Triple(520,1180,60)),bounds)!!
  assertEquals(300f,p.first,3f);assertEquals(1250f,p.second,3f)
 }
 @Test fun twoTouchingButtonsAreSplit(){
  val p=DashButtonLocator.locate(w,h,frame(Triple(300,1200,60),Triple(415,1200,60)),bounds)!!
  assertEquals(300f,p.first,15f)
 }
 @Test fun noButtonReturnsNull(){assertNull(DashButtonLocator.locate(w,h,frame(),bounds))}
}
