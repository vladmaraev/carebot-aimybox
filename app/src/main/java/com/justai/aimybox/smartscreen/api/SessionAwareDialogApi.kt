package com.justai.aimybox.smartscreen.api

import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.api.aimybox.AimyboxRequest
import com.justai.aimybox.api.aimybox.AimyboxResponse
import com.justai.aimybox.core.CustomSkill

class SessionAwareDialogApi(
    apiKey: String,
    unitId: String,
    override val customSkills: LinkedHashSet<CustomSkill<AimyboxRequest, AimyboxResponse>> = linkedSetOf()
): DialogApi<AimyboxRequest, AimyboxResponse>() {

    private val dialogApi = AimyboxDialogApi(apiKey, unitId, customSkills = customSkills)

    override fun createRequest(query: String) = dialogApi.createRequest(query)

    override suspend fun send(request: AimyboxRequest) = dialogApi.send(request)

    suspend fun resetSession() = send(createRequest("/reset"))
}