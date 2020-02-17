package com.justai.aimybox.smartscreen.extensions

import com.justai.aimybox.components.widget.ImageWidget

val ImageWidget.isVideo: Boolean
    get() = this.url.contains(".mp4")