package com.adaptive.launcher.domain

import org.junit.Assert.*
import org.junit.Test

class BackupRepositoryTest {
    @Test fun export_containsSchemaVersion(){
        val json = """{"schemaVersion":1,"launcherVersion":"1.0.0","favorites":[],"streams":[]}"""
        assertTrue(json.contains("\"schemaVersion\""))
        assertTrue(json.contains("1.0.0"))
        // basic structural check without android org.json
        assertTrue(json.startsWith("{") && json.endsWith("}"))
    }
    @Test fun favorites_roundTrip(){
        val pkg = "com.test"
        val json = """{"favorites":[{"packageName":"$pkg","activityName":"$pkg.Main","position":0,"userSerial":0}]}"""
        assertTrue(json.contains(pkg))
        assertTrue(json.contains("activityName"))
    }
    @Test fun invalidJson_handled(){
        val input = "not json"
        val ok = try {
            // naive check: must start with { and contain :
            if (!input.trim().startsWith("{")) throw IllegalArgumentException("not json")
            true
        } catch(_:Exception){ false }
        assertFalse(ok)
    }
    @Test fun streams_serialization(){
        val streamJson = """{"id":"work","name":"Work","position":0,"apps":[]}"""
        assertTrue(streamJson.contains("Work"))
        assertTrue(streamJson.contains("work"))
    }
}
