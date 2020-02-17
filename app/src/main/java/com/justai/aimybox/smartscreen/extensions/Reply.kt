package com.justai.aimybox.smartscreen.extensions

import com.justai.aimybox.components.widget.*
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ImageReply
import com.justai.aimybox.model.reply.TextReply

val ButtonsReply.asWidget: ButtonsWidget
    get() = this.buttons.map { button ->
        val url = button.url
        val payload = button.payload
        when {
            url != null -> LinkButton(button.text, url)
            payload != null -> PayloadButton(button.text, payload)
            else -> ResponseButton(button.text)
        }
    }.let { ButtonsWidget(it) }


val TextReply.asWidget: ResponseWidget
    get() = ResponseWidget(this.text)

val ImageReply.asWidget: ImageWidget
    get() = ImageWidget(this.url)