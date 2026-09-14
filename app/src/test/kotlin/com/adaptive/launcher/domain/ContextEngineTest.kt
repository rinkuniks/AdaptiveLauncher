package com.adaptive.launcher.domain

import com.adaptive.launcher.domain.context.ContextEngine
import com.adaptive.launcher.domain.context.ContextSignal
import org.junit.Assert.*
import org.junit.Test

class ContextEngineTest {
    private val engine = ContextEngine()
    @Test fun score_ordersByRecencyAndFrequency(){
        val now = 1_000_000L
        val signals = listOf(
            ContextSignal("com.a", now - 1000, 10, 2, 2),
            ContextSignal("com.b", now - 1000*3600*24, 1, 0, 0),
        )
        val ranked = engine.score(signals, now, 2, 2)
        assertEquals("com.a", ranked.first().packageName)
        assertTrue(ranked.first().score > ranked.last().score)
    }
    @Test fun timeBucket_bonus(){
        val now = 1_000_000L
        val signals = listOf(
            ContextSignal("com.a", now - 10000, 1, 5, 3),
            ContextSignal("com.b", now - 10000, 1, 1, 3),
        )
        val ranked = engine.score(signals, now, 5, 3)
        assertEquals("com.a", ranked.first().packageName)
    }
    @Test fun empty_returnsEmpty(){ assertTrue(engine.score(emptyList(), 0, 0, 0).isEmpty()) }
    @Test fun capsAtSix(){
        val now = 1_000_000L
        val signals = (0..10).map{ ContextSignal("com.$it", now - it*1000, it, 0, 0)}
        assertTrue(engine.score(signals, now, 0, 0).size <= 6)
    }
}
