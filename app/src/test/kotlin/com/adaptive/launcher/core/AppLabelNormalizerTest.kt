package com.adaptive.launcher.core

import com.adaptive.launcher.core.common.AppLabelNormalizer
import org.junit.Assert.*
import org.junit.Test

class AppLabelNormalizerTest {
    @Test fun normalize_lowercasesAndTrims(){ assertEquals("chrome", AppLabelNormalizer.normalize("  Chrome ")) }
    @Test fun normalize_stripsDiacritics(){ assertEquals("cafe", AppLabelNormalizer.normalize("Café")) }
    @Test fun normalize_handlesEmpty(){ assertEquals("", AppLabelNormalizer.normalize("")) }
    @Test fun section_aToZ(){ assertEquals('C', AppLabelNormalizer.sectionFor("chrome")) }
    @Test fun section_numericIsHash(){ assertEquals('#', AppLabelNormalizer.sectionFor("123app")) }
    @Test fun section_symbolIsHash(){ assertEquals('#', AppLabelNormalizer.sectionFor("#cool")) }
    @Test fun section_emptyIsHash(){ assertEquals('#', AppLabelNormalizer.sectionFor("")) }
    @Test fun normalize_unicode(){ assertEquals("uber", AppLabelNormalizer.normalize("ÜBER")) }
}
