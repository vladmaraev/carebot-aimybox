package com.justai.aimybox.smartscreen.skill

import com.justai.aimybox.api.aimybox.AimyboxRequest
import com.justai.aimybox.api.aimybox.AimyboxResponse
import com.justai.aimybox.core.CustomSkill
import java.util.*

class DateTimeSkill: CustomSkill<AimyboxRequest, AimyboxResponse> {

    override suspend fun onRequest(request: AimyboxRequest): AimyboxRequest {
        val offset = Calendar.getInstance().timeZone.rawOffset
        request.data?.addProperty("offset", offset / 60000)
        return request
    }
}