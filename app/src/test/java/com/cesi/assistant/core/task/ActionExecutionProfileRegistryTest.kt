package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionExecutionProfileRegistryTest {
    private val registry = ActionExecutionProfileRegistry()

    @Test
    fun directCallIsExecutableAndPermissionAware() {
        val profile = registry.profileFor(AssistantIntent.Call("Ahmed"))
        assertEquals(ExecutionSupport.EXECUTABLE, profile.support)
        assertEquals(VerificationMode.EXTERNAL_SIDE_EFFECT, profile.verification)
        assertTrue(profile.requiresRuntimePermission)
    }

    @Test
    fun dialIsExplicitlyPartial() {
        val profile = registry.profileFor(AssistantIntent.Dial("123456"))
        assertEquals(ExecutionSupport.PARTIAL, profile.support)
        assertEquals(VerificationMode.UI_OPEN_ONLY, profile.verification)
        assertFalse(profile.requiresRuntimePermission)
    }

    @Test
    fun ussdDialerFlowDoesNotRequireCallPermission() {
        val profile = registry.profileFor(AssistantIntent.Ussd("*123#"))
        assertEquals(ExecutionSupport.PARTIAL, profile.support)
        assertEquals(VerificationMode.UI_OPEN_ONLY, profile.verification)
        assertFalse(profile.requiresRuntimePermission)
    }

    @Test
    fun messageIsNotClaimedAsAutomaticSend() {
        val profile = registry.profileFor(AssistantIntent.Message("Aisha", "Zan zo yanzu"))
        assertEquals(ExecutionSupport.PARTIAL, profile.support)
        assertEquals(VerificationMode.UI_OPEN_ONLY, profile.verification)
        assertTrue(profile.requiresRuntimePermission)
    }

    @Test
    fun unknownIntentIsNeverExecutable() {
        val profile = registry.profileFor(AssistantIntent.Unknown)
        assertEquals(ExecutionSupport.NOT_IMPLEMENTED, profile.support)
        assertEquals(VerificationMode.NOT_AVAILABLE, profile.verification)
    }
}
