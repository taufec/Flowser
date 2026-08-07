package com.flowser.app.browser

import java.net.URI
import java.util.Locale

internal object TickTickCompatibility {
    fun shouldInject(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase(Locale.US) }.getOrNull() ?: return false
        return host == "ticktick.com" || host.endsWith(".ticktick.com")
    }

    fun script(): String = """
        (function () {
          if (window.__flowserTickTickCheckboxShim) return;
          window.__flowserTickTickCheckboxShim = true;

          function checkboxWidget(target) {
            var node = target && target.nodeType === 1 ? target : null;
            while (node && node !== document.documentElement) {
              if (node.classList && node.classList.contains('CodeMirror-widget')) {
                var useNode = node.querySelector('use');
                if (!useNode) return null;
                var href = useNode.getAttribute('href') || useNode.getAttribute('xlink:href') || '';
                return href.indexOf('md-item-') >= 0 ? node : null;
              }
              node = node.parentElement;
            }
            return null;
          }

          function replayMouse(target, touch) {
            ['mousedown', 'mouseup', 'click'].forEach(function (type) {
              target.dispatchEvent(new MouseEvent(type, {
                bubbles: true,
                cancelable: true,
                view: window,
                detail: 1,
                clientX: touch.clientX,
                clientY: touch.clientY,
                screenX: touch.screenX,
                screenY: touch.screenY,
                button: 0,
                buttons: type === 'mousedown' ? 1 : 0
              }));
            });
          }

          document.addEventListener('touchend', function (event) {
            if (!event.changedTouches || event.changedTouches.length !== 1) return;

            var touch = event.changedTouches[0];
            var hit = document.elementFromPoint(touch.clientX, touch.clientY) || event.target;
            var widget = checkboxWidget(hit) || checkboxWidget(event.target);
            if (!widget) return;

            event.preventDefault();
            event.stopImmediatePropagation();
            replayMouse(hit || widget, touch);
          }, { capture: true, passive: false });
        })();
    """.trimIndent()
}
