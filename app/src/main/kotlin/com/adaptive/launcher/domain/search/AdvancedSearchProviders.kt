package com.adaptive.launcher.domain.search

import com.adaptive.launcher.core.model.LauncherApp
import javax.inject.Inject

class SearchCoordinator @Inject constructor(private val appProvider: AppSearchProvider){
    suspend fun search(query: String, apps: List<LauncherApp>): List<SearchProvider.ScoredApp> {
        val q = query.trim()
        if(q.isEmpty()) return emptyList()
        val results = mutableListOf<SearchProvider.ScoredApp>()
        results.addAll(appProvider.search(q, apps))
        return results
    }
}

class CalculatorProvider {
    fun evaluate(query: String): String? {
        try {
            val q = query.trim()
            if(q.isEmpty()) return null
            if(!q.any { it.isDigit() }) return null
            val allowed = setOf('+','-','*','/','(',')','.',' ')
            if(q.any { !it.isDigit() && it !in allowed }) return null
            val sanitized = q.replace(" ","")
            val opIdx = sanitized.indexOfFirst { it == '+' || it == '-' || it == '*' || it == '/' }
            if(opIdx <= 0) return null
            val op = sanitized[opIdx]
            val a = sanitized.substring(0, opIdx).toDoubleOrNull() ?: return null
            val b = sanitized.substring(opIdx+1).toDoubleOrNull() ?: return null
            val r = when(op){
                '+'->a+b
                '-'->a-b
                '*'->a*b
                '/'-> if(b!=0.0) a/b else return null
                else->return null
            }
            return if(r % 1.0 == 0.0) r.toLong().toString() else r.toString()
        } catch(_:Exception){ return null }
    }
}
