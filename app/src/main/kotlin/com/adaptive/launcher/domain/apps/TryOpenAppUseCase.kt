package com.adaptive.launcher.domain.apps

import com.adaptive.launcher.data.apps.AppMetaRepository
import javax.inject.Inject

sealed class TryOpenAppResult { object Launch : TryOpenAppResult(); object ShowChallenge : TryOpenAppResult() }

class TryOpenAppUseCase @Inject constructor(private val appMetaRepository: AppMetaRepository) {
    suspend operator fun invoke(packageName: String, bypassChallenge: Boolean = false): TryOpenAppResult {
        if (bypassChallenge) return TryOpenAppResult.Launch
        return if (appMetaRepository.isChallenge(packageName)) TryOpenAppResult.ShowChallenge else TryOpenAppResult.Launch
    }
}
