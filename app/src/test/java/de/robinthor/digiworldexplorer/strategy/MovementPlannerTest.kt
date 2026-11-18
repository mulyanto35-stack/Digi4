package de.robinthor.digiworldexplorer.strategy
import de.robinthor.digiworldexplorer.detection.*
import org.junit.Assert.*
import org.junit.Test
class MovementPlannerTest{
 private fun board(obstacles:Set<Cell> = emptySet(),items:Set<Cell> = emptySet())=(0..4).flatMap{r->(0..4).map{c->Cell(r,c)}}.associateWith{c->CellScores(0.0,if(c in items).1 else 0.0,0.0,0.0,if(c in items).1 else 0.0,if(c in obstacles).5 else 0.0,0.0)}
 @Test fun prefersRight(){assertEquals(Cell(2,3),MovementPlanner.choose(Cell(2,2),board(),emptyList())?.target)}
 @Test fun routesToItem(){val a=MovementPlanner.choose(Cell(2,2),board(items=setOf(Cell(1,2))),emptyList());assertEquals(Cell(1,2),a?.target);assertEquals(ActionKind.MOVE,a?.kind)}

 /** Free row: several rightward steps are planned in one go. */
 @Test fun burstsThreeStepsOnClearRow(){
  val p=MovementPlanner.plan(Cell(2,0),board(),emptyList(),maxSteps=3)
  assertEquals(listOf(Cell(2,1),Cell(2,2),Cell(2,3)),p.map{it.target})
 }

 /** If the game swallows a tap, the chain shifts by one cell. If a pyramid stands in this safety
  *  column, nothing is bundled - otherwise a claw would be lost. */
 @Test fun noBurstWhenPyramidCouldBeHitAfterMissedTap(){
  val p=MovementPlanner.plan(Cell(2,0),board(setOf(Cell(2,3))),emptyList(),claws=5,maxSteps=3)
  assertEquals(1,p.size)
 }

 /** Towards a collectable the path is bundled, not just the first step. */
 @Test fun burstsAlongItemRoute(){
  val p=MovementPlanner.plan(Cell(2,0),board(items=setOf(Cell(2,3))),emptyList(),maxSteps=2)
  assertEquals(listOf(Cell(2,1),Cell(2,2)),p.map{it.target})
 }

 /** Smashing is never bundled - the image must be re-evaluated afterwards. */
 @Test fun neverBurstsAttacks(){
  val walls=(0..4).map{Cell(it,2)}.toSet()
  val p=MovementPlanner.plan(Cell(2,1),board(walls,setOf(Cell(2,3))),emptyList(),claws=3,maxSteps=3)
  assertEquals(1,p.size);assertEquals(ActionKind.ATTACK,p[0].kind)
 }

 /** On the way to the item, smashing only happens if there is no way around. Here the adjacent
  *  rows are built up, so the breakthrough is the only affordable way. */
 @Test fun attacksBlockingPyramidOnItemRouteWhenWalledIn(){
  val walls=(0..4).map{Cell(it,2)}.toSet()
  val a=MovementPlanner.choose(Cell(2,1),board(walls,setOf(Cell(2,3))),emptyList(),claws=3)
  assertEquals(Cell(2,2),a?.target);assertEquals(ActionKind.ATTACK,a?.kind)
 }

 /** If a short detour is available, the pyramid stays standing - even with a full supply. */
 @Test fun prefersDetourOverBreakingWithManyClaws(){
  val a=MovementPlanner.choose(Cell(2,1),board(setOf(Cell(2,2)),setOf(Cell(2,3))),emptyList(),claws=5)
  assertEquals(ActionKind.MOVE,a?.kind)
 }

 /** With only the reserve, the pyramid is bypassed to the right instead of smashed. */
 @Test fun avoidsPyramidWithOnlyReserveClaw(){
  val a=MovementPlanner.choose(Cell(2,2),board(setOf(Cell(2,3))),emptyList(),claws=1)
  assertEquals(ActionKind.MOVE,a?.kind);assertNotEquals(Direction.RIGHT,a?.direction)
 }

 /** An unknown number is treated as cautiously as "almost empty". */
 @Test fun avoidsPyramidWhenCountUnknown(){
  assertEquals(ActionKind.MOVE,MovementPlanner.choose(Cell(2,2),board(setOf(Cell(2,3))),emptyList(),claws=null)?.kind)
 }

 /** If really no cell is free anymore, the reserve claw is used. */
 @Test fun spendsReserveWhenCompletelyBoxedIn(){
  val walls=setOf(Cell(1,2),Cell(3,2),Cell(2,1),Cell(2,3))
  val a=MovementPlanner.choose(Cell(2,2),board(walls),emptyList(),claws=1)
  assertEquals(ActionKind.ATTACK,a?.kind);assertTrue(a?.reason.orEmpty().contains("Reserve"))
 }

 /** Without claws a pyramid is a wall - then better no move at all than an ineffective tap. */
 @Test fun neverAttacksWithoutClaws(){
  val walls=setOf(Cell(1,2),Cell(3,2),Cell(2,1),Cell(2,3))
  assertNull(MovementPlanner.choose(Cell(2,2),board(walls),emptyList(),claws=0))
 }

 @Test fun breaksOscillationAtRightEdge(){val h=listOf(Cell(4,3),Cell(4,4),Cell(4,3),Cell(4,4));assertEquals(Direction.UP,MovementPlanner.choose(Cell(4,4),board(),h)?.direction)}

 /** With a clear path the look-ahead counts: the row with three free cells wins. */
 @Test fun prefersRowWithClearLookahead(){
  val blockedRow=setOf(Cell(2,2),Cell(2,3),Cell(2,4))
  val a=MovementPlanner.choose(Cell(2,1),board(blockedRow),emptyList())
  assertNotEquals(Direction.RIGHT,a?.direction)
 }

 /** One Dash charge is kept as a reserve for a genuine stuck state. */
 @Test fun noDashWithoutReason(){
  val b=board(setOf(Cell(2,2),Cell(2,3)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),b,emptyList(),dashAvailable=true,dashCharges=1)?.kind)
 }

 /** Even with two pyramids no Dash is used as long as a free detour exists. */
 @Test fun noDashWhenTwoPyramidsHaveDetour(){
  val b=board(setOf(Cell(2,1),Cell(2,2)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),b,emptyList(),dashAvailable=true,dashCharges=2)?.kind)
 }

 /** Two pyramids ahead and a complete wall: now Dash is truly forced. */
 @Test fun dashOnlyWhenWallHasNoDetour(){
  val walls=(0..4).flatMap{r->listOf(Cell(r,1),Cell(r,2))}.toSet()
  assertEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),board(walls),emptyList(),dashAvailable=true,dashCharges=2)?.kind)
 }

 /** Visible energy takes priority; the bot must not Dash past it. */
 @Test fun noDashPastVisibleEnergy(){
  val b=board(setOf(Cell(2,1),Cell(2,2)),setOf(Cell(4,4)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),b,emptyList(),dashAvailable=true,dashCharges=2)?.kind)
 }

 /** Position pacing alone does not use up a Dash if a free path still exists. */
 @Test fun noDashWhenStuckButFreeRouteExists(){
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,2),board(),emptyList(),dashAvailable=true,stuck=true)?.kind)
 }

 /** Without an available Dash, it stays with the normal move. */
 @Test fun noDashWhenUnavailable(){
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,2),board(),emptyList(),dashAvailable=false,stuck=true)?.kind)
 }

 /** A long free detour always beats a short attack path. */
 @Test fun neverAttacksWhenLongDetourExists(){
  val walls=setOf(Cell(2,1),Cell(1,1),Cell(3,1))
  val a=MovementPlanner.choose(Cell(2,0),board(walls,setOf(Cell(2,2))),emptyList(),claws=5)
  assertEquals(ActionKind.MOVE,a?.kind)
  assertNotEquals(Cell(2,1),a?.target)
 }

 /** In a three-cell pocket the bot follows the breakout path and attacks instead of pacing. */
 @Test fun attacksOutOfClosedPocket(){
  val wall=(0..4).map{Cell(it,2)}.toSet()
  val a=MovementPlanner.choose(Cell(4,1),board(wall),emptyList(),claws=1)
  assertEquals(ActionKind.ATTACK,a?.kind)
  assertEquals(Cell(4,2),a?.target)
 }

 /** Without a claw, only the confirmed emergency Dash remains in a complete pocket. */
 @Test fun emergencyDashWhenTrappedWithoutClaw(){
  val wall=(0..4).map{Cell(it,2)}.toSet()
  val a=MovementPlanner.choose(Cell(2,1),board(wall),emptyList(),dashAvailable=true,dashCharges=1,claws=0)
  assertEquals(ActionKind.DASH,a?.kind)
 }

 /** Equivalent items ahead of the player are collected before a nearer way back. */
 @Test fun prefersForwardItemOverBacktracking(){
  val a=MovementPlanner.choose(Cell(2,2),board(items=setOf(Cell(2,1),Cell(2,4))),emptyList())
  assertEquals(Direction.RIGHT,a?.direction)
 }

 /** In a pocket a reachable item is taken first and then a targeted breakout follows. */
 @Test fun collectsPocketItemBeforeBreakingOut(){
  val wall=(0..4).map{Cell(it,2)}.toSet()
  val a=MovementPlanner.choose(Cell(4,1),board(wall,setOf(Cell(3,1))),emptyList(),claws=1)
  assertEquals(ActionKind.MOVE,a?.kind)
  assertEquals(Cell(3,1),a?.target)
 }

 /** A side cell without its own exit is not entered if the outer path is visible. */
 @Test fun neverEntersVisibleCornerBeforeDetour(){
  val player=Cell(3,1)
  val walls=setOf(Cell(3,2),Cell(2,2),Cell(4,0),Cell(4,2))
  val base=board(walls).toMutableMap()
  base[Cell(4,1)]=CellScores(0.0,0.0,0.0,0.0,0.0,0.0,1.0)
  val a=MovementPlanner.choose(player,base,emptyList(),claws=4)
  assertEquals(Direction.UP,a?.direction)
  assertNotEquals(Cell(4,1),a?.target)
 }
 /** An adjacent collectable beats one further right but more distant. */
 @Test fun adjacentItemBeatsFarForwardItem(){
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(1,1),Cell(2,4))),emptyList())
  assertEquals(Direction.UP,a?.direction)
  assertEquals(Cell(1,1),a?.target)
 }

 @Test fun forwardProfileNeverMovesLeft(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true)
  val a=MovementPlanner.choose(Cell(2,2),board(items=setOf(Cell(2,1))),emptyList(),settings=settings)
  assertEquals(Direction.RIGHT,a?.direction)
 }

 @Test fun forwardProfileAttacksObstacleDirectlyAhead(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true)
  val a=MovementPlanner.choose(Cell(2,1),board(obstacles=setOf(Cell(2,2))),emptyList(),claws=2,settings=settings)
  assertEquals(ActionKind.ATTACK,a?.kind)
  assertEquals(Cell(2,2),a?.target)
  assertEquals(Direction.RIGHT,a?.direction)
 }

 @Test fun forwardProfileStillTargetsVisibleEnergy(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true)
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(1,1))),emptyList(),settings=settings)
  assertEquals(ActionKind.MOVE,a?.kind)
  assertEquals(Cell(1,1),a?.target)
  assertEquals(Direction.UP,a?.direction)
 }

 @Test fun dashSpamUsesOnlyConfirmedPositiveCounter(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true,dashSpamUntilZero=true)
  assertEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),board(),emptyList(),dashAvailable=true,dashCharges=2,settings=settings)?.kind)
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),board(),emptyList(),dashAvailable=true,dashCharges=0,settings=settings)?.kind)
 }

 @Test fun visibleEnergySuppressesDashSpam(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true,dashSpamUntilZero=true)
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(1,1))),emptyList(),dashAvailable=true,dashCharges=3,settings=settings)
  assertNotEquals(ActionKind.DASH,a?.kind)
  assertEquals(Cell(1,1),a?.target)
 }

 @Test fun betterCollectTakesSameColumnEnergyBeforeCloserForwardEnergy(){
  val settings=DwsNavigationSettings(collectOnlyEnergy=true,betterEnergyCollect=true)
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(0,1),Cell(2,2))),emptyList(),settings=settings)
  assertEquals(Direction.UP,a?.direction)
  assertEquals(Cell(1,1),a?.target)
 }

 @Test fun onlyEnergyIgnoresOtherCollectables(){
  val settings=DwsNavigationSettings(collectOnlyEnergy=true)
  val cells=board(items=setOf(Cell(1,1))).toMutableMap()
  cells[Cell(1,1)]=cells.getValue(Cell(1,1)).copy(orange=0.0)
  val a=MovementPlanner.choose(Cell(2,1),cells,emptyList(),settings=settings)
  assertNotEquals(Cell(1,1),a?.target)
  assertEquals(Direction.RIGHT,a?.direction)
 }
}
